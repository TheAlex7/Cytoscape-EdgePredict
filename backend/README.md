## Running Backend
### Make sure docker is running 
```
docker info
```

### Build docker container
```
docker build -t blant-predict .
```

#### **Important note for WSL users**: For allowing docker desktop into WSL terminal go to ```settings > resources > enable WSL integration``` and enable Ubuntu.

### Run BLANT-Predict Server on local host
```
docker run -p 127.0.0.1:49161:5000 blant-predict  // bind to loop-back IP address
```

##### NOTE: <local_host_port>:<internal_docker_port>

### To save job data to a path use a volume mount
```
docker run -p 127.0.0.1:49161:5000 -v host/machine/job/dir:/app/jobs blant-predict
```

### Docker Image Environment variables
The docker image uses the following environment variables to determine resources of the gunicorn server:
- WORKERS (default 1) 
- THREADS (default 4)

Set them with: ```docker run -e WORKERS=8 -e IP_ADDRESS="0.0.0.0" -p 49161:5000 -v host/machine/job/dir:/app/jobs blant-predict```
### API Documentation
```
<url_path>/blant
```
- **Description**: Accepts graph file to start a new BLANT job. The job_id is calculated as the sha256 checksum of the input file and then returned.
- **query params** ```k```, ```sampling_method``` (Note: sampling method is not validated so can cause error if not valid for blant. Other blant parameters still in progress.)
- **request body** ```file```
- **returns** ```string``` 

```
<url_path>/progress/<job_id>
```
- **Description**: Check on progress of program by giving percentage. Currently is only boolean 0 or 1.
- **returns** ```float```

```
<url_path>/stderr_stream/<job_id>
```
- **Description**: Forms a connection through Server-Sent Events (SSE) for real-time updates on program stderr. Blant outputs program dialogue here.
- **yields** ```text/event-stream``` 

```
<url_path>/results/<job_id>
```
- **Description**: Gives list of predicted edges from BLANT-Predict. (Currently gives raw stdout, parsing in progress)
- **response content** ```<upload_filename>_blant_res.txt``` (text/plain)

```
<url_path>/clear/<job_id>
```
- **Description**: Removes all info from specified job.

```
<url_path>/clear-all
```
- **Description**: Removes all info from all jobs.