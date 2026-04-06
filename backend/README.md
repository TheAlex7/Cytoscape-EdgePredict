Important note: for allowing docker desktop into wsl terminal go to ```settings > resources > enable WSL integration``` and enable Ubuntu.
### Make sure docker is running 
```docker info```

### Build docker container
```docker build -t flask-blant .```

### Run Flask Server on local host
```docker run -p 49161:5000 flask-blant```

### To quickly test small changes to code using current dir (requires local binaries, run make)

```docker run -p 49161:5000 -v $(pwd):/app flask-blant```

### NOTE: <local_host_port>:<internal_docker_port>

### API Documentation
```<url_path>/blant```
- **Description**: Accepts graph file to start a new BLANT job. The job_id is calculated as the sha256 checksum of the input file and then returned.
- **query params** ```k```, ```sampling_method``` (Note: sampling method is not validated so can cause error if not valid for blant. Other blant parameters still in progress.)
- **request body** ```file```
- **returns** ```string``` 

```<url_path>/progress/<job_id>```
- **Description**: Check on progress of program by giving percentage. Currently is only boolean 0 or 1.
- **returns** ```float```

```<url_path>/stderr_stream/<job_id>```
- **Description**: Forms a connection through Server-Sent Events (SSE) for real-time updates on program stderr. Blant outputs program dialogue here.
- **yields** ```text/event-stream``` 

```<url_path>/results/<job_id>```
- **Description**: Gives raw stdout of the BLANT program.
- **response content** ```<upload_filename>_blant_res.txt``` (text/plain)

```<url_path>/clear/<job_id>```
- **Description**: Removes all info from specified job.

```<url_path>/clear-all```
- **Description**: Removes all info from all jobs.