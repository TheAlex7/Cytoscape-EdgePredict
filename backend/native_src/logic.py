import subprocess, threading, os

def run_blant(jobs, job_id, input_path, out_path):
    if not job_id in jobs:
        return -1 
    process_data = jobs[job_id]

    process_data["finished"] = False

    # TODO: fix bug where threads are overlapping
    process = subprocess.Popen(
        ["bash", "./native_src/scripts/run_mock.sh", input_path],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1
    )

    # Thread to read stderr continuously through queue
    def read_stderr():
        for line in process.stderr:
            process_data["stderr_queue"].put(line)
        process.stderr.close()

    stderr_thread = threading.Thread(target=read_stderr)
    stderr_thread.start()
    stdout, _ = process.communicate()
    stderr_thread.join()

    # stdout_file = stdout.encode('utf-8')
    try:
        with open(out_path, 'w', encoding='utf-8') as file:
            file.write(stdout)
    except IOError as e:
        print(f"Error writing to file: {e}")

    process_data["finished"] = True