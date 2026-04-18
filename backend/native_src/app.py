from flask import Flask, request, jsonify, redirect, url_for, send_file, Response
import io
import threading
import queue
import os
# import uuid
from native_src.logic import *
import shutil

app = Flask(__name__)

# file size limit 1 MB (1024 * 1024 bytes)
app.config['MAX_CONTENT_LENGTH'] = 1024 * 1024 # NOTE: 1mb chosen since thats how hayes lab website is
VALID_EXTENSIONS = {'txt', 'csv', 'sif', 'el'}
JOBS_FOLDER = "jobs"
os.makedirs(JOBS_FOLDER, exist_ok=True)

# Store running jobs globally TODO: switch to completely file based jobs
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
    "base_name": String
}
"""

@app.route('/')
def index():
    return redirect(url_for('sayHello'))

@app.route('/hello', methods=['GET'])
def sayHello():
    return jsonify({"message": "hello world!"}), 200

# error message for files exceeding the size limit
@app.errorhandler(413)
def fileTooLarge(error):
    return jsonify({"error": "File is too large. Max limit is 1MB"}), 413

@app.route("/clear-all")
def flushAll():
    shutil.rmtree(JOBS_FOLDER)
    os.makedirs(JOBS_FOLDER, exist_ok=True)

    jobs.clear()
    return jsonify({"message": "successfully cleared all job data"}), 200

@app.route("/clear/<job_id>")
def flushJob(job_id):
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400

    os.remove(os.path.join(JOBS_FOLDER, job_id))
    del jobs[job_id]
    return jsonify({"message": "successfully cleared specified job data"}), 200

@app.route("/stderr_stream/<job_id>")
def getStderr(job_id):
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400
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

# TODO: add parsing + parameters to make it easy on frontend
@app.route("/results/<job_id>")
def getResult(job_id):
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400
    process_data = jobs[job_id]

    if not process_data["finished"]:
        return jsonify({"error": "Job not done."}), 400
    
    base_name = process_data["base_name"]

    buffer = io.BytesIO()
    with open(process_data["stdout_path"], "r", encoding="utf-8") as file:
        buffer.write(file.read().encode("utf-8"))
    buffer.seek(0)

    return send_file(
        buffer,
        as_attachment=True,
        download_name=f'{base_name}_blant_res.txt',
        mimetype='text/plain'
    )

# for now it'll give float 0, or 1.0 to represent "done" / "not done"
# (future goal) TODO: progress should be an estimate depending on current epoch and precision
@app.route("/progress/<job_id>")
def checkProgress(job_id): 
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400
    process_data = jobs[job_id]

    if not process_data["finished"]:
        return jsonify({"progress": 0})

    return jsonify({"progress": 1})

def isValidFile(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in VALID_EXTENSIONS

# TODO: add ALL program parameters
@app.route("/blant", methods=["POST"])
def startBlant():
    if 'file' not in request.files:
        return jsonify({"error": "No file provided"}), 400
    
    file = request.files['file']

    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    # file validation
    if not isValidFile(file.filename):
        return jsonify({"error": f"Invalid file type. Allowed: {VALID_EXTENSIONS}"}), 400

    k = request.args.get("k", default="4", type=str)        # default k=4
    sampling_method = request.args.get("method", default="MCMC") #TODO: confirm default method and add valid method checker
    isForced = request.args.get("force", default="0", type=str)

    if not k.isnumeric() or int(k) < 3 or int(k) > 8:
        return jsonify({"error": f"k must be integer in range [3,8]."}), 400
    
    if not isForced.isnumeric():
        return jsonify({"error": f"force must be integer representation of a boolean."}), 400
    
    # extension includes "." (ex: .gml, .el, .txt), if no extension then empty str
    user_ext = "." + file.filename.rsplit('.', 1)[1].lower() if "." in file.filename else ""

    job_id = get_checksum(file) # should be the checksum of the upload file
    os.makedirs(os.path.join(JOBS_FOLDER, job_id), exist_ok=True)

    ### Checking if job has already been computed can be done on frontend instead
    path = f"{JOBS_FOLDER}/{job_id}"
    # if isForced == "0" and (job_id in jobs or os.path.isdir(path)): # not a forced job and it already exists
    #     return jsonify({"error" : f"Job has already been computed inside job {job_id} [truncated].",
    #                     "jobID" : job_id}), 409

    upload_path = os.path.join(JOBS_FOLDER, job_id, "upload" + user_ext) #<job_id>/upload.<user_file_ext>
    stdout_path = os.path.join(JOBS_FOLDER, job_id, "stdout.txt")
    stderr_path = os.path.join(JOBS_FOLDER, job_id, "stderr.txt")
    file.save(upload_path)

    cleaned_base_name = file.filename.rsplit('.', 1)[0].lower().replace(".", "_") # just in case given weird file names

    jobs[job_id] = {
        "finished": False, 
        "stderr_queue": queue.Queue(),
        "upload_path": upload_path,
        "stdout_path": stdout_path,
        "stderr_path": stderr_path,
        "base_name": cleaned_base_name
    }

    thread = threading.Thread(
        target=run_blant,
        args=(jobs, job_id, upload_path, stdout_path, stderr_path, k, sampling_method)
    )
    thread.start() # separate thread to continue handling other calls

    return jsonify({"job_id": job_id})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)