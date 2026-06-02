import subprocess, threading, os
import hashlib
import json
import os
import select
import re

def run_blant(job_data_path, input_path, stdout_path, stderr_path, k="4", sampling_method = "EBE!", precision = "1.5", MOCK=False):
    job_data = load_job_data(job_data_path)

    # if job_data.get("aborted"):
    #     return

    if not precision.isdigit(): 
        precision = f"{float(precision)}"

    job_data["finished"] = False
    update_job_data(job_data)

    if MOCK:
        COMMAND = ["bash", "./native_src/scripts/run_mock.sh", "./native_src/mock/syeast0_stderr_k4mcmc.txt", "./native_src/mock/syeast0_stdout_k4mcmc.txt"]
    else:
        COMMAND = ["bash", "./src/EdgePredict/scripts/predict-edges-from-network.sh", "-s", sampling_method, "-p", precision, input_path, k ]
    
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
            while process.poll() is None: # returns None when process hasn't finished
                tmp_job_data = load_job_data(job_data_path)
                if not tmp_job_data and tmp_job_data.get("aborted"):
                    process.terminate()
                    job_data["aborted"] = 2
                    update_job_data(job_data)
                    break
                ready, _, _ = select.select([process.stderr],[],[],1)
                if ready:
                    line = process.stderr.readline()
                    if not line: break
                    update_progress(line, job_data)
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
        if os.path.isfile(stdout_path) and empty:
            os.remove(stdout_path)
        
    stderr_thread = threading.Thread(target=stream_stderr)
    stdout_thread = threading.Thread(target=capture_stdout)

    stderr_thread.start()
    stdout_thread.start()

    stderr_thread.join()
    stdout_thread.join()
    process.wait()

    if not os.path.isfile(job_data["stdout_path"]): #stdout doesn't exist = error happened
        job_data["error"] = True
    else:
        job_data["finished"] = True
    update_job_data(job_data)

def get_checksum(file_storage, algorithm="sha256", chunk_size=65536) -> str:
    hasher = hashlib.new(algorithm)
    
    file_storage.seek(0) # Reset pointer
    
    while True:
        data = file_storage.read(chunk_size) # Read in 64kb chunks
        if not data:
            break
        hasher.update(data)
    
    file_storage.seek(0) 
    
    return hasher.hexdigest()

def parse_line(blant_line: str, file_format) -> str:
    # if file_format == "sif":
    #     # sif format line: src\tpredicted\tGSG1\t0.966667\t4:11:11\n']
    #     src, _, dest, confidence, orbit = blant_line.split("\t")
    # else:
    
    """ default line: 'src:dest\tprec\tcount bestCol orbit\n' """

    nodes, confidence, orbit_info = blant_line.split("\t")
    src, dest = nodes.split(":")
    src = reverse_character_mapping(src)
    dest = reverse_character_mapping(dest)
    
    count, BEST_COL, orbit = orbit_info.split(" ") # I assume the number is the count of the particular orbit observed
        
    return "\t".join([ elem.strip() for elem in (src, dest, confidence, orbit)]) + "\n" # not sure if we should include count

def reverse_character_mapping(node_name):
    """ 
    As per Prof Hayes, node names are encoded as such:
        underscores => ^U (control-U character), 
        spaces => underscores,
        colons => ^F (control-F character)
    """
    # Replace Underscores with spaces
    node_name = node_name.replace('_', ' ')

    # Replace Control-U (\x15) with an underscore
    node_name = node_name.replace('\x15', '_')

    # Replace Control-F (\x06) with colon
    node_name = node_name.replace('\x06', ':')

    return node_name

def update_job_data(data, filepath=None):
    if not filepath:
        filepath = data["job_data_path"]
        # print(filepath)
    updated_data = data.copy()
    if "stderr_queue" in updated_data:
        del updated_data["stderr_queue"] # not serializable
    if "process" in updated_data:
        del updated_data["process"]
    with open(filepath, "w") as file:
        json.dump(updated_data, file)

def load_job_data(filepath):
    try:
        with open(filepath, "r", encoding="utf-8") as file:
            return json.load(file)
    except json.decoder.JSONDecodeError:
        return None

def update_progress(stderr_line, job_data):
    # Pattern looking for "batch <int>" and "(<float> digits)"
    pattern = r"\bbatch\s+(\d+).*?\(( *-?[\d.]+)\s+digits\)"
    match = re.search(pattern, stderr_line)
    if match:
        REQUIRED_BATCHES = 10
        new_progress = 0.0
        batch = int(match.group(1))
        digits = float(match.group(2))
        target_prec = job_data.get("target_prec", 1.5)

        if digits > target_prec:
            new_progress = batch/REQUIRED_BATCHES
        else:
            new_progress = digits/target_prec

        print(f"WHATS UP NEW PROGRESS {new_progress}")

        job_data["progress"] = max(job_data.get("progress", 0.0), new_progress, 0.0)
        job_data["progress"] = min(job_data["progress"], .99) # clip to .99 since 1 should be when finished=True
        update_job_data(job_data)

def progress_tostring(progress: float):
    if progress >= 1:
        return "99%"
    elif progress < 0:
        return "0%"
    else:
        return str(int(progress*100)) + "%"

def is_empty_file(filepath):
    with open(filepath, 'r') as file:
        if not file.read().strip():
            return True
        
    return False

def extract_file_ext(filename):
    if '.' in filename:
        return filename.rsplit('.', 1)[1].lower()
    return "" # no extension