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

#### To save job data and/or support SSL (HTTPS), use volume mounts
```
docker run -p 127.0.0.1:49161:5000 \
    -v /path/to/local/jobs:/app/jobs \
    -v /path/to/local/keys:/app/keys \
    thealex7/blant-predict
```

##### **NOTE:** keyfile and certfile must be in the same directory and named `key.pem` and `cert.pem` respectively

### Docker Image Environment variables
The docker image uses the following environment variables to determine resources of the gunicorn server:
- WORKERS (default 2) 
- THREADS (default 4)

Set them with: ```docker run -e WORKERS=8 -e IP_ADDRESS="0.0.0.0" -p 49161:5000 -v host/machine/job/dir:/app/jobs blant-predict```

## API Documentation
```
<url_path>/blant
```
- **Description**: Accepts graph file to start a new BLANT job. The job_id is calculated as the sha256 checksum of the input file and then returned.
- **query params** `k`, `sampling_method`, `precision`
- **request body** ```file```
- **returns** ```string``` 

```
<url_path>/progress/<job_id>
```
- **Description**: Check on progress of program by giving percentage. Currently is only boolean 0 or 1.
- **returns** ```float```

```
<url_path>/stderr/<job_id>
```
- **Description**: Sends a text file of the current stderr of the BLANT-Predict program.
- **response content** ```stderr.txt``` (text/plain)

```
<url_path>/results/<job_id>
```
- **Description**: Gives list of predicted edges from BLANT-Predict. (Currently gives raw stdout, parsing in progress)
- **query params**
    - `raw_out`: `Bool` When `True`, gives the raw stdout of the BLANT-Predict program otherwise the output is parsed into columns of `node1`, `node2`, `confidence score`, `predictive orbit`
- **response content** ```result.txt``` (text/plain)

```
<url_path>/clear/<job_id>
```
- **Description**: Removes all info from specified job.