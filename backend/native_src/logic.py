import subprocess, threading, os
import hashlib

def run_blant(jobs, job_id, input_path, stdout_path, stderr_path, k="4", sampling_method = "MCMC"):
    if not job_id in jobs:
        return -1 
    process_data = jobs[job_id]

    process_data["finished"] = False

    process = subprocess.Popen(
        # ["bash", "./native_src/scripts/run_mock.sh", "./native_src/mock/syeast_stderr.txt", "./native_src/mock/mock_output.txt"], # comment this when running BLANT
        # ["bash", "./native_src/scripts/run_blant.sh", "-k", k, "-s", sampling_method, "-mp", input_path], # uncomment this when running BLANT
        ["bash", "./src/EdgePredict/scripts/predict-edges-from-network.sh", "-s", sampling_method, input_path, k ], # uncomment this when running BLANT
        cwd = "/app",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1
    )

    def stream_stderr():
        with open(stderr_path, "w") as f:
            for line in process.stderr:
                process_data["stderr_queue"].put(line) # streaming line
                f.write(line) # saving output
                f.flush()

    def capture_stdout():
        with open(stdout_path, "w") as f:
            for line in process.stdout:
                f.write(line)
                f.flush()

    stderr_thread = threading.Thread(target=stream_stderr)
    stdout_thread = threading.Thread(target=capture_stdout)

    stderr_thread.start()
    stdout_thread.start()

    stderr_thread.join()
    stdout_thread.join()
    process.wait()

    process_data["finished"] = True

def get_checksum(file_storage, algorithm="sha256", chunk_size=65536):
    hasher = hashlib.new(algorithm)
    
    file_storage.seek(0) # Reset pointer
    
    while True:
        data = file_storage.read(chunk_size) # Read in 64kb chunks
        if not data:
            break
        hasher.update(data)
    
    file_storage.seek(0) 
    
    return hasher.hexdigest()