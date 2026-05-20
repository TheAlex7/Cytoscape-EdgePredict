from flask import Flask, request, jsonify, send_file, Response
import io
import threading
# import queue
import os
from native_src.logic import *
import shutil
# import uuid

app = Flask(__name__)

# file size limit 1 MB (1024 * 1024 bytes)
VALID_EXTENSIONS = {'txt', 'csv', 'sif', 'el'}
VALID_SAMPLING_METHODS = {"MCMC", "NBE", "EBE!", "RES!", "AR!", "FAYE!", "INDEX", "EDGE_COVER"}
JOBS_FOLDER = "jobs"
JOB_DATA_FILENAME = "job_data.json"
os.makedirs(JOBS_FOLDER, exist_ok=True)

"""
# Jobs should be in this format:
job_data = {
    "error": Bool,
    "finished": Bool,
    "upload_path": String,
    "stdout_path": String,
    "stderr_path": String,
    "job_data_path": job_data_path,
    "file_format": String
}
"""

@app.route('/')
def index():
    return jsonify({"message": "Server Running."}), 200

# error message for files exceeding the size limit
@app.errorhandler(413)
def fileTooLarge(error):
    return jsonify({"error": "File is too large"}), 413 # No set file limit at the moment

@app.route("/clear/<job_id>")
def flushJob(job_id):
    job_path = os.path.join(JOBS_FOLDER, job_id)

    if not os.path.isdir(job_path): # not job_id in jobs and 
        return jsonify({"error": "Job not found."}), 400

    if os.path.isdir(job_path):
        shutil.rmtree(job_path)
    
    return jsonify({"message": "Successfully cleared specified job data."}), 200

# TODO: save spot of last checked line + add param for returning whole file
@app.route("/stderr/<job_id>")
def getStderr(job_id):
    job_data_path = os.path.join(JOBS_FOLDER, job_id, JOB_DATA_FILENAME)

    if os.path.isfile(job_data_path):
        job_data = load_job_data(job_data_path)
    else:
        return jsonify({"error": "Job not found."}), 400
    
    stream = request.args.get("stream", default="0", type=str)

    #### deprecated for now until a good method is found. For now it is not important
    # if stream == "1":
    #     # stream file data according to 
    #     pass

    buffer = io.BytesIO()
    with open(job_data["stderr_path"], "r", encoding="utf-8") as file:
        buffer.write(file.read().encode("utf-8"))
    buffer.seek(0)

    return send_file(
        buffer,
        as_attachment=True,
        download_name='user_network_stderr.txt',
        mimetype='text/plain'
    )

    # def generate():
    #     while not process_data["finished"] or not process_data["stderr_queue"].empty():
    #         try:
    #             line = process_data["stderr_queue"].get(timeout=0.5)
    #             if line is None:  # terminates
    #                 break
    #             yield f"data: {line}\n\n"
    #         except queue.Empty:
    #             continue
    #         except:
    #             return "data: BLANT encountered an Error."
    #     yield "data: [PREDICTION COMPLETE]\n\n"

    # return Response(generate(), mimetype="text/event-stream", headers={'X-Accel-Buffering': 'no'})

@app.route("/results/<job_id>")
def getResult(job_id):
    job_data_path = os.path.join(JOBS_FOLDER, job_id, JOB_DATA_FILENAME)
    if os.path.isfile(job_data_path):
        job_data = load_job_data(job_data_path)
    else:
        return jsonify({"error": "Job not found."}), 400
    
    raw_output = request.args.get("raw_out", default="0", type=str)
        
    if job_data["error"]:
        return jsonify({"error" : "Job encountered an error."}), 500
    if not job_data["finished"]:
        return jsonify({"error": "Job not done."}), 400

    buffer = io.BytesIO()
    with open(job_data["stdout_path"], "r", encoding="utf-8") as file:
        if raw_output == "1":
            buffer.write(file.read().encode("utf-8"))
        else:
            for line in file:
                formatted_line = parse_line(line, job_data["file_format"])
                buffer.write(formatted_line.encode("utf-8"))
    buffer.seek(0)

    return send_file(
        buffer,
        as_attachment=True,
        download_name='user_network_res.txt',
        mimetype='text/plain'
    )

# gives boolean for if job is finished + current precision (PENDING/TODO)
@app.route("/progress/<job_id>")
def checkProgress(job_id):
    job_data_path = os.path.join(JOBS_FOLDER, job_id, JOB_DATA_FILENAME)
    if os.path.isfile(job_data_path):
        job_data = load_job_data(job_data_path)
    else:
        return jsonify({"error": "Job not found."}), 400

    if job_data["error"]:
        return jsonify({"progress": 0, "error": "Job encounterd an error"}), 500
    elif not job_data["finished"]:
        return jsonify({"progress": 0})

    return jsonify({"progress": 1})

# TODO: job abort
# TODO: Jobs should be grouped by checksum of input and subfolders are the unique param config (low priority)
@app.route("/blant", methods=["POST"])
def startBlant():
    if 'file' not in request.files:
        return jsonify({"error": "No file provided."}), 400
    
    file = request.files['file']

    if file.filename == '':
        return jsonify({"error": "No selected file."}), 400

    file_format = extract_file_ext(file.filename)

    # file validation
    if file_format not in VALID_EXTENSIONS:
        return jsonify({"error": f"Invalid file type. Allowed: {VALID_EXTENSIONS}"}), 400

    k = request.args.get("k", default="4", type=str)        # default k=4
    sampling_method = request.args.get("method", default="EBE!").upper()
    isForced = request.args.get("force", default="0", type=str) # boolean
    isMock = request.args.get("mock", default="0", type=str) # boolean
    precision = request.args.get("precision", default="1.5", type=str) # digit of precision 

    if sampling_method not in VALID_SAMPLING_METHODS:
        return jsonify({"error": f"Not a valid sampling method. Valid sampling methods: MCMC, NBE, EBE!, RES!, AR!, FAYE!, INDEX, EDGE_COVER"}), 400

    if not k.isdigit() or int(k) < 3 or int(k) > 8:
        return jsonify({"error": f"k must be integer in range [3,8]."}), 400
    
    if not isForced.isdigit():
        return jsonify({"error": f"force must be integer representation of a boolean."}), 400
    
    # if the precision param is invalid, won't throw error but will treat it as default precision
    try:
        if float(precision) <= 0:
            precision = "1"
        elif precision.isdigit():
            precision = str(int(precision)) # get rid of leading 0's
        else:
            precision = str(float(precision))
    except ValueError:
        precision = "1"
    
    # extension includes "." (ex: .sif, .el, .txt), if no extension then empty str
    user_ext = "." + file_format if file_format != "" else ""

    # should be the checksum of the upload file + params
    job_id = get_checksum(file) + k + sampling_method + precision.replace(".", "-")

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

    job_data = {
        # "stderr_queue" : queue.Queue(),
        "finished": False,
        "error": False,
        "upload_path": upload_path,
        "stdout_path": stdout_path,
        "stderr_path": stderr_path,
        "job_data_path": job_data_path,
        "file_format": file_format
    }

    update_job_data(job_data, job_data_path)

    thread = threading.Thread(
        target=run_blant,
        args=(job_data_path, upload_path, stdout_path, stderr_path, k, sampling_method, precision, isMock=="1")
    )
    thread.start() # separate thread to continue handling other calls

    return jsonify({"job_id": job_id})

# @app.route("/abort/<job_id>", methods=["POST"])
# def abortJob(job_id):
#     if job_id not in jobs:
#         return jsonify({"error": "Running job not found."}), 404

#     job_data = jobs[job_id]

#     if job_data["finished"]:
#         return jsonify({"error": "Job already finished."}), 400

#     job_data["aborted"] = True

#     process = job_data.get("process")
#     if process is not None and process.poll() is None:
#         process.terminate()

#     job_data["finished"] = True
#     job_data["stderr_queue"].put(None)
#     update_job_data(job_data, job_data["job_data_path"])

#     return jsonify({"message": "Job aborted."}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)