Important note: for allowing docker desktop into wsl terminal go to ```settings > resources > enable WSL integration``` and enable Ubuntu.
### Make sure docker is running 
```docker info```

### Build docker container
```docker build -t flask-c-bridge .```

### Run Flask Server on local host
```docker run -p 49161:5000 flask-c-bridge```

### To quickly test small changes to code using current dir (requires local binaries, run make)

```docker run -p 49161:5000 -v $(pwd):/app flask-c-bridge```

## NOTE: <local_host_port>:<internal_docker_port>