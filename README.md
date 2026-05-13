# Cytoscape-EdgePredict
This project is concerned with the wide deployment of [Professor Hayes'](https://github.com/waynebhayes) BLANT-Predict algorithm used for the edge prediction on a given network. We mainly provide access to BLANT-Predict through a plugin on [Cytoscape](https://cytoscape.org/) (a popular network analysis tool) but we also provide a [Docker image of our backend API](https://hub.docker.com/r/thealex7/blant-predict) which can be used locally or on a personal server. 

Consequently, this allows for the use of the BLANT-Predict API by services outside of Cytoscape.

The [Frontend](https://github.com/TheAlex7/Cytoscape-EdgePredict/tree/main/frontend) folder contains the logic for the Cytoscape plugin and the [Backend](https://github.com/TheAlex7/Cytoscape-EdgePredict/tree/main/backend) folder contains API logic while omitting proprietary BLANT-Predict source code. As such, a fully functional docker image cannot be built without a working BLANT-Predict directory inside ```/backend/BLANT```, which is not publicly available.

## Quick Setup (Pre-Appstore Deployment)
**Prerequisite:** Must have [Cytoscape](https://cytoscape.org/) installed.

```
git clone https://github.com/TheAlex7/Cytoscape-EdgePredict.git CSEP
cd CSEP
```

Open the frontend and backend directories in separate windows.
### Running Frontend
``` 
cd frontend
```

#### 1. Install all dependencies
```
pip install -r requirements.txt
```

#### 2. Build application using Maven

```
mvn clean install
```
This commend will build `.jar` file in `./target` folder.

#### 3. Move the .jar file into Cytoscape Configuration folder.

Your Cytoscape Configuration folder is usually located in your home directory. Locate `CytoscapeConfiguration`, go to `.\3\apps\installed`, and move jar file there. 

Cytoscape will automatically install BLANT Prediction into your Cytoscape.

#### Note
BLANT Prediction will also save user input/output caches in `CytoscapeConfiguration\3\apps\BLANT` after running the task if the user agrees to save input and output.

### Running Backend (Optional)
The Cytoscape frontend connects to our publicly hosted Docker Image endpoints by default. We recommend sticking with our servers but if you need a way to run the program offline or want to use it on your own server, you may do this by pulling [our Docker image](https://hub.docker.com/r/thealex7/blant-predict) from DockerHub.

**Prerequisite:** Must have [Docker](https://docs.docker.com/get-started/get-docker/) installed

#### Make sure docker is running 
```
docker info
```

##### **Important note for WSL users**: For allowing docker desktop into WSL terminal go to ```settings > resources > enable WSL integration``` and enable Ubuntu.

#### Pull our Docker Image
```
docker pull thealex7/blant-predict:v1
```

#### Run BLANT-Predict Server on local host (or any address+port)
```
docker run -p 127.0.0.1:49161:5000 thealex7/blant-predict:v1
```

##### **NOTE:** <host_machine_port>:<internal_docker_port>

#### To save job data to a path use a volume mount
```
docker run -p 127.0.0.1:49161:5000 -v host/machine/job/dir:/app/jobs thealex7/blant-predict:v1
```

## API Documentation
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
