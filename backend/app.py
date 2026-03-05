import subprocess
from flask import Flask, request, jsonify, redirect, url_for, send_file
import io

app = Flask(__name__)

@app.route('/')
def index():
    return redirect(url_for('sayHello'))

@app.route('/hello', methods=['GET'])
def sayHello():
    
    try:
        # Execute shell script
        result = subprocess.Popen(['bash', './say_hello.sh'],
                                capture_output=True, 
                                text=True)
        
        return jsonify({
            "status": "success",
            "message_from_c": result.stdout.strip() # C's stdout
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500
    

# file size limit 1 MB (1024 * 1024 bytes)
app.config['MAX_CONTENT_LENGTH'] = 1024 * 1024
VALID_EXTENSIONS = {'txt', 'csv', 'sif'}

def isValidFile(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in VALID_EXTENSIONS

# error message for files exceeding the size limit
@app.errorhandler(413)
def fileTooLarge(error):
    return jsonify({"error": "File is too large. Max limit is 1MB"}), 413

@app.route('/blant', methods=['POST'])
def sendToBlant():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400
    
    file = request.files['file']

    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    if not isValidFile(file.filename):
        return jsonify({"error": f"Invalid file type. Allowed: {VALID_EXTENSIONS}"}), 400

    try:
        graph_file = file.read().decode('utf-8') # we don't do anything with it just yet
        blant_result = subprocess.run(['bash', './run_mock.sh'],
                                    capture_output=True, 
                                    text=True)

        if blant_result.returncode != 0:
            return jsonify({
                "error": "blant process failed",
                "stderr": blant_result.stderr
            }), 500
        
        buffer = io.BytesIO()
        buffer.write(blant_result.stdout.encode('utf-8'))
        buffer.seek(0)

        return send_file(
            buffer,
            as_attachment=True,
            download_name=f'{file.filename}blant_res',
            mimetype='text/plain'
        )
    except Exception as e:
        return jsonify({"error": "Failed to process text file. Ensure it is UTF-8 encoded.",
                        "raw": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)