import subprocess
from flask import Flask, request, jsonify, redirect, url_for, send_file, Response
import io
import threading
import queue
import os
import uuid
from src.logic import *
import shutil

app = Flask(__name__)

# file size limit 1 MB (1024 * 1024 bytes)
# app.config['MAX_CONTENT_LENGTH'] = 1024 * 1024 # actually not needed locally for now
VALID_EXTENSIONS = {'txt', 'csv', 'sif'}
UPLOAD_FOLDER = "uploads"
STDOUT_FOLDER = "job_results" # might be useful later
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Store running jobs locally
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
    
    try:
        # Execute shell script
        result = subprocess.run(['bash', './scripts/say_hello.sh'],
                                capture_output=True, 
                                text=True)
        
        return jsonify({
            "status": "success",
            "message_from_c": result.stdout.strip() # C's stdout
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# error message for files exceeding the size limit
@app.errorhandler(413)
def fileTooLarge(error):
    return jsonify({"error": "File is too large. Max limit is 1MB"}), 413

# in order to support progress checking, had to split up the current 
# blant call into multiple calls as per RESTful API design
@app.route("/results/<job_id>")
def getResultAndFlush(job_id):
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400
    process_data = jobs[job_id]
    base_name = process_data["base_name"]

    buffer = io.BytesIO()
    buffer.write(process_data["stdout"])
    buffer.seek(0)

    del jobs[job_id]
    shutil.rmtree(UPLOAD_FOLDER)

    return send_file(
        buffer,
        as_attachment=True,
        download_name=f'{base_name}_blant_res.txt',
        mimetype='text/plain'
    )


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


@app.route("/progress/<job_id>")
def checkProgress(job_id): # for now it'll give float 0, or 1.0 to represent "done" / "not done"
    if not job_id in jobs:
        return jsonify({"error": "Job not found."}), 400
    process_data = jobs[job_id]

    if not process_data["finished"]:
        return jsonify({"progress": 0})

    return jsonify({"progress": 1})

def isValidFile(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in VALID_EXTENSIONS


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
    job_id = str(uuid.uuid4())
    file_path = os.path.join(UPLOAD_FOLDER, job_id + "_" + file.filename.rsplit('.', 1)[1].lower()) #<job_id>_<user_file_ext>
    file.save(file_path)

    jobs[job_id] = {
        "finished": False, 
        "stderr_queue": queue.Queue(), 
        "process": None,
        "file_path": file_path,
        "base_name": file.filename.rsplit('.', 1)[0].lower()
    }

    thread = threading.Thread(
        target=run_blant,
        args=(jobs, job_id, file_path)
    )
    thread.start() # separate thread to continue handling other calls

    return jsonify({"job_id": job_id})

### deprecated
# @app.route('/blant', methods=['POST'])
# def sendToBlant():
#     if 'file' not in request.files:
#         return jsonify({"error": "No file part"}), 400
    
#     file = request.files['file']

#     if file.filename == '':
#         return jsonify({"error": "No selected file"}), 400

#     if not isValidFile(file.filename):
#         return jsonify({"error": f"Invalid file type. Allowed: {VALID_EXTENSIONS}"}), 400

#     try:
#         graph_file = file.read().decode('utf-8') # we don't do anything with it just yet
#         blant_result = subprocess.run(['bash', './run_mock.sh'],
#                                     capture_output=True, 
#                                     text=True)

#         if blant_result.returncode != 0:
#             return jsonify({
#                 "error": "blant process failed",
#                 "stderr": blant_result.stderr
#             }), 500
        
#         buffer = io.BytesIO()
#         buffer.write(blant_result.stdout.encode('utf-8'))
#         buffer.seek(0)

#         return send_file(
#             buffer,
#             as_attachment=True,
#             download_name=f'{file.filename.rsplit('.', 1)[0].lower()}_blant_res.txt',
#             mimetype='text/plain'
#         )
#     except Exception as e:
#         return jsonify({"error": "Failed to process text file. Ensure it is UTF-8 encoded.",
#                         "raw": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)