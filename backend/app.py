import subprocess
from flask import Flask, request, jsonify, redirect, url_for

app = Flask(__name__)

@app.route('/')
def index():
    return redirect(url_for('sayHello'))

@app.route('/hello', methods=['GET'])
def sayHello():
    
    try:
        # Execute the C binary
        result = subprocess.run(['./hello'], 
                                capture_output=True, 
                                text=True)
        
        return jsonify({
            "status": "success",
            "message_from_c": result.stdout.strip() # C's stdout
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)