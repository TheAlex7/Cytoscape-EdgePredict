from flask import Flask, request, jsonify, redirect, url_for, send_file, Response
import io
import threading
import queue
import os
from native_src.logic import *
import shutil
# import uuid

app = Flask(__name__)

# file size limit 1 MB (1024 * 1024 bytes)
app.config['MAX_CONTENT_LENGTH'] = 1024 * 1024 # NOTE: 1mb chosen since thats how hayes lab website is
VALID_EXTENSIONS = {'txt', 'csv', 'sif', 'el'}
VALID_SAMPLING_METHODS = {"MCMC", "NBE", "EBE!", "RES!", "AR!", "FAYE!", "INDEX", "EDGE_COVER"}
JOBS_FOLDER = "jobs"
JOB_DATA_FILENAME = "job_data.json"
os.makedirs(JOBS_FOLDER, exist_ok=True)

# Store running jobs globally
global jobs 
jobs = dict()
"""
# Jobs should be in this format:
process_data = {
    "finished": Bool,
    "stderr_queue": queue.Queue(),
    "upload_path": String,
    "stdout_path": String,
    "stderr_path": String,
    "job_data_path": job_data_path,
    "base_name": String
}
"""

@app.route('/')
def index():
    return jsonify({"message": "Server Running."}), 200

# error message for files exceeding the size limit
@app.errorhandler(413)
def fileTooLarge(error):
    return jsonify({"error": "File is too large. Max limit is 1MB."}), 413

# # Used for testing only
# @app.route("/clear-all")
# def flushAll():
#     for job_dir in os.listdir(JOBS_FOLDER):
#         job_path = os.path.join(JOBS_FOLDER, job_dir)
#         try:
#             shutil.rmtree(job_path)
#             jobs.clear()
#         except Exception as e:
#             print(f"Failed to delete {job_path}. Reason: {e}")
#             return jsonify({"message": "job data not cleared",
#                             "error" : e}), 500    
#     return jsonify({"message": "successfully cleared all job data"}), 200

@app.route("/clear/<job_id>")
def flushJob(job_id):
    job_path = os.path.join(JOBS_FOLDER, job_id)

    if not job_id in jobs and not os.path.isdir(job_path):
        return jsonify({"error": "Job not found."}), 400
    
    if job_id in jobs:
        del jobs[job_id]

    if os.path.isdir(job_path):
        shutil.rmtree(job_path)
    
    return jsonify({"message": "Successfully cleared specified job data."}), 200

@app.route("/stderr_stream/<job_id>")
def getStderr(job_id):
    if not job_id in jobs:
        return jsonify({"error": "Running job not found."}), 400
    process_data = jobs[job_id]

    def generate():
        while not process_data["finished"] or not process_data["stderr_queue"].empty():
            try:
                line = process_data["stderr_queue"].get(timeout=0.5)
                if line is None:  # terminates
                    break
                yield f"data: {line}\n\n"
            except:
                continue
        yield "data: [PREDICTION COMPLETE]\n\n"

    return Response(generate(), mimetype="text/event-stream")

@app.route("/results/<job_id>")
def getResult(job_id):
    job_data_path = os.path.join(JOBS_FOLDER, job_id, JOB_DATA_FILENAME)
    if job_id in jobs:
        job_data = jobs[job_id]
    elif os.path.isfile(job_data_path):
        job_data = load_job_data(job_data_path)
    else:
        return jsonify({"error": "Job not found."}), 400
    
    raw_output = request.args.get("raw_out", default="0", type=str)
        
    if "error" in job_data:
        return jsonify({"error" : "Job encountered an error."}), 500
    if not job_data["finished"]:
        return jsonify({"error": "Job not done."}), 400

    base_name = job_data["base_name"]

    buffer = io.BytesIO()
    with open(job_data["stdout_path"], "r", encoding="utf-8") as file:
        if raw_output == "1":
            buffer.write(file.read().encode("utf-8"))
        else:
            for line in file:
                formatted_line = parse_line(line)
                buffer.write(formatted_line.encode("utf-8"))
    buffer.seek(0)

    return send_file(
        buffer,
        as_attachment=True,
        download_name=f'{base_name}_blant_res.txt',
        mimetype='text/plain'
    )

# gives boolean for if job is finished + current precision (PENDING/TODO)
@app.route("/progress/<job_id>")
def checkProgress(job_id):
    job_data_path = os.path.join(JOBS_FOLDER, job_id, JOB_DATA_FILENAME)
    if job_id in jobs:
        job_data = jobs[job_id]
    elif os.path.isfile(job_data_path):
        job_data = load_job_data(job_data_path)
    else:
        return jsonify({"error": "Job not found."}), 400

    if not job_data["finished"]:
        return jsonify({"progress": 0})

    return jsonify({"progress": 1})

def isValidFile(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in VALID_EXTENSIONS

# TODO: add ALL program parameters
@app.route("/blant", methods=["POST"])
def startBlant():
    if 'file' not in request.files:
        return jsonify({"error": "No file provided."}), 400
    
    file = request.files['file']

    if file.filename == '':
        return jsonify({"error": "No selected file."}), 400

    # file validation
    if not isValidFile(file.filename):
        return jsonify({"error": f"Invalid file type. Allowed: {VALID_EXTENSIONS}"}), 400

    k = request.args.get("k", default="4", type=str)        # default k=4
    sampling_method = request.args.get("method", default="MCMC").upper()
    isForced = request.args.get("force", default="0", type=str) # boolean
    isMock = request.args.get("mock", default="0", type=str) # boolean

    ### TODO
    # precision = request.args.get("precision", default="-1", type=str) # greater than 0
    # num_samples = request.args.get("num_samples", default="-1", type=str) # greater than 0
    # include_known = request.args.get("include_known", default="0", type=str) # boolean
    ###

    if sampling_method not in VALID_SAMPLING_METHODS:
        return jsonify({"error": f"Not a valid sampling method. Valid sampling methods: MCMC, NBE, EBE!, RES!, AR!, FAYE!, INDEX, EDGE_COVER"}), 400

    if not k.isnumeric() or int(k) < 3 or int(k) > 8:
        return jsonify({"error": f"k must be integer in range [3,8]."}), 400
    
    if not isForced.isnumeric():
        return jsonify({"error": f"force must be integer representation of a boolean."}), 400
    
    # extension includes "." (ex: .gml, .el, .txt), if no extension then empty str
    user_ext = "." + file.filename.rsplit('.', 1)[1].lower() if "." in file.filename else ""

    job_id = get_checksum(file) # should be the checksum of the upload file

    ### Checking if job has already been computed can be done on frontend instead
    job_path = os.path.join(JOBS_FOLDER, job_id)

    os.makedirs(job_path, exist_ok=True)
    upload_path = os.path.join(job_path, "upload" + user_ext) #<job_id>/upload<.user_file_ext>
    stdout_path = os.path.join(job_path, "stdout.txt")
    stderr_path = os.path.join(job_path, "stderr.txt")
    job_data_path = os.path.join(job_path, JOB_DATA_FILENAME)

    if isForced == "0" and os.path.isfile(stdout_path): # not a forced job and it already exists
        return jsonify({"error" : f"Job is already running or has already been computed.",
                        "job_id" : job_id}), 409

    file.save(upload_path)

    cleaned_base_name = file.filename.rsplit('.', 1)[0].lower().replace(".", "_") # just in case given weird file names

    job_data = {
        "finished": False,
        "stderr_queue" : queue.Queue(),
        "upload_path": upload_path,
        "stdout_path": stdout_path,
        "stderr_path": stderr_path,
        "job_data_path": job_data_path,
        "base_name": cleaned_base_name
    }

    update_job_data(job_data, job_data_path)

    jobs[job_id] = job_data

    thread = threading.Thread(
        target=run_blant,
        args=(jobs, job_id, upload_path, stdout_path, stderr_path, k, sampling_method, True)
    )
    thread.start() # separate thread to continue handling other calls

    return jsonify({"job_id": job_id})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)