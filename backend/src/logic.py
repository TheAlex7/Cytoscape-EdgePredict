import subprocess
import threading


def run_blant(jobs, job_id, file_path):
    if not job_id in jobs:
        return -1
    process_data = jobs[job_id]
    process_data["finished"] = False

    process = subprocess.Popen(
        ["bash", "./run_mock.sh", file_path],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        bufsize=1
    )

    process_data["process"] = process
    stdout_lines = []

    def read_stdout():
        for line in process.stdout:
            print(f"[STDOUT] {line.rstrip()}", flush=True)
            stdout_lines.append(line)

    def read_stderr():
        for line in process.stderr:
            print(f"[STDERR] {line.rstrip()}", flush=True)
            process_data["stderr_queue"].put(line.rstrip())

    stdout_thread = threading.Thread(target=read_stdout)
    stderr_thread = threading.Thread(target=read_stderr)

    stdout_thread.start()
    stderr_thread.start()

    stdout_thread.join()
    stderr_thread.join()

    process.wait()

    print(f"[DEBUG] stdout_lines length: {len(stdout_lines)}", flush=True)

    process_data["stdout"] = "".join(stdout_lines).encode("utf-8")
    process_data["finished"] = True