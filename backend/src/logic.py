import subprocess, threading
def run_blant(jobs, job_id, file_path):
    if not job_id in jobs:
        return -1 
    process_data = jobs[job_id]

    process_data["finished"] = False

    process = subprocess.Popen(
        ["bash", "./scripts/run_mock.sh", file_path],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1
    )

    process_data["process"] = process

    # Thread to read stderr continuously through queue
    def read_stderr():
        for line in process.stderr:
            process_data["stderr_queue"].put(line)
        process.stderr.close()

    stderr_thread = threading.Thread(target=read_stderr)
    stderr_thread.start()
    stdout, _ = process.communicate()
    stderr_thread.join()

    process_data["stdout"] = stdout.encode('utf-8') # if large outputs give error we can shift to saving to a temp file and serving it
    process_data["finished"] = True