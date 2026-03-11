import subprocess
from flask import Flask, request, jsonify, redirect, url_for, send_file, Response
import io
import threading
import queue
import os
import uuid
from native_src.logic import *
import shutil

app = Flask(__name__)

# file size limit 1 MB (1024 * 1024 bytes)
# app.config['MAX_CONTENT_LENGTH'] = 1024 * 1024 # actually not needed for now
VALID_EXTENSIONS = {'txt', 'csv', 'sif', 'el'} # TODO: double check which files are allowed, maybe get rid of extension validator?
UPLOAD_FOLDER = "uploads"
RESULTS_FOLDER = "job_results"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(RESULTS_FOLDER, exist_ok=True)

# Store running jobs globally
jobs = dict()
"""
# Jobs should be in this format:
process_data = {
    "stderr_queue": queue.Queue(),
    "process": process | None,
    "finished": Bool,
    "file_path": String,
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

@app.route("/results/<job_id>")
def getResult(job_id):
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400
    process_data = jobs[job_id]
    base_name = process_data["base_name"]

    buffer = io.BytesIO()
    with open(process_data["result_path"], "r", encoding="utf-8") as file:
        buffer.write(file.read().encode("utf-8"))
    buffer.seek(0)

    return send_file(
        buffer,
        as_attachment=True,
        download_name=f'{base_name}_blant_res.txt',
        mimetype='text/plain'
    )

@app.route("/clear-all")
def flushAll():
    shutil.rmtree(UPLOAD_FOLDER)
    shutil.rmtree(RESULTS_FOLDER)
    del jobs
    jobs = dict()

@app.route("/clear/<job_id>")
def flushJob(job_id):
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400

    os.remove(jobs[job_id]["upload_path"])
    os.remove(jobs[job_id]["result_path"])
    del jobs[job_id]

@app.route("/blant_stderr/<job_id>")
def getStderr(job_id):
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400
    process_data = jobs[job_id]

    def generate():
        while not process_data["finished"] or not process_data["stderr_queue"].empty():
            try:
                line = process_data["stderr_queue"].get(timeout=0.5)
                yield f"data: {line}\n\n"
            except:
                continue
        yield "data: [PREDICTION COMPLETE]\n\n"

    return Response(generate(), mimetype="text/event-stream")

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

# TODO: add program parameters
@app.route("/blant", methods=["POST"])
def startBlant():
    if 'file' not in request.files:
        return jsonify({"error": "No file provided"}), 400
    
    file = request.files['file']

    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    if not isValidFile(file.filename):
        return jsonify({"error": f"Invalid file type. Allowed: {VALID_EXTENSIONS}"}), 400

    file = request.files["file"]
    job_id = str(uuid.uuid4())#[:5] # shortened for development
    upload_path = os.path.join(UPLOAD_FOLDER, job_id + "." + file.filename.rsplit('.', 1)[1].lower()) #<job_id>.<user_file_ext>
    result_path = os.path.join(RESULTS_FOLDER, job_id)
    file.save(upload_path)

    cleaned_base_name = file.filename.rsplit('.', 1)[0].lower().replace(".", "_") # just in case given weird file names

    jobs[job_id] = {
        "finished": False, 
        "stderr_queue": queue.Queue(),
        "upload_path": upload_path,
        "result_path": result_path,
        "base_name": cleaned_base_name
    }

    thread = threading.Thread(
        target=run_blant,
        args=(jobs, job_id, upload_path, result_path)
    )
    thread.start() # separate thread to continue handling other calls

    return jsonify({"job_id": job_id})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)