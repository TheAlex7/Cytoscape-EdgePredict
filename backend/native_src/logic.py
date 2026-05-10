import subprocess, threading
import hashlib
import json
import os

def run_blant(job_data_path, input_path, stdout_path, stderr_path, k="4", sampling_method = "EBE!", MOCK=False):
    process_data = load_job_data(job_data_path)

    process_data["finished"] = False
    update_job_data(process_data, process_data["job_data_path"])

    if MOCK:
        COMMAND = ["bash", "./native_src/scripts/run_mock.sh", "./native_src/mock/syeast0_stderr_k4mcmc.txt", "./native_src/mock/syeast0_stdout_k4mcmc.txt"]
    else:
        COMMAND = ["bash", "./src/EdgePredict/scripts/predict-edges-from-network.sh", "-s", sampling_method, input_path, k ]
    
    process = subprocess.Popen(
        COMMAND,
        cwd = "/app",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1
    )

    def stream_stderr():
        with open(stderr_path, "w") as f:
            for line in process.stderr:
                # process_data["stderr_queue"].put(line) # streaming line # deprecated
                f.write(line) # saving output
                f.flush()

    def capture_stdout():
        empty = True
        with open(stdout_path, "w") as f:
            for line in process.stdout:
                f.write(line)
                f.flush()
                empty = False

        # makes sure empty file doesn't persist
        if empty:
            os.remove(stdout_path)
        

    stderr_thread = threading.Thread(target=stream_stderr)
    stdout_thread = threading.Thread(target=capture_stdout)

    stderr_thread.start()
    stdout_thread.start()

    stderr_thread.join()
    stdout_thread.join()
    process.wait()

    if not os.path.isfile(process_data["stdout_path"]): #stdout doesn't exist = error happened
        process_data["error"] = True
    else:
        process_data["finished"] = True
    update_job_data(process_data, process_data["job_data_path"])

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

def parse_line(blant_line: str) -> str:
    # print(blant_line) # raw line: 'src:dest\tprec\tcount bestCol orbit\n'
    nodes, confidence, orbit_info = blant_line.split("\t")
    src, dest = nodes.split(":")
    count, BEST_COL, orbit = orbit_info.split(" ") # I assume the number is the count of the particular orbit observed
    return "\t".join([ elem.strip() for elem in (src, dest, confidence, orbit)]) + "\n" # not sure if we should include count

def update_job_data(data, filepath):
    updated_data = data.copy()
    if "stderr_queue" in updated_data:
        del updated_data["stderr_queue"] # not serializable
    with open(filepath, "w") as file:
        json.dump(updated_data, file)

def load_job_data(filepath):
    with open(filepath, "r", encoding="utf-8") as file:
        return json.load(file)
    
def is_empty_file(filepath):
    with open(filepath, 'r') as file:
        if not file.read().strip():
            return True
        
    return False