# How to build and install BLANT Prediction
 
Core dependencies: `Maven`

## 1. Install all dependencies

```
pip install -r requirements.txt
```

## 2. Build application using Maven

```
mvn clean install
```
This commend will build `.jar` file in `./target` folder.

## 3. Move the .jar file into Cytoscape Configuration folder.

Your Cytoscape Configuration folder is usually located in your home directory. Locate `CytoscapeConfiguration`, go to `.\3\apps\installed`, and move jar file there. 

Cytoscape will automatically install BLANT Prediction into your Cytoscape.

### Note
BLANT Prediction will also save user input/output caches in `CytoscapeConfiguration\3\apps\BLANT` after running the task if the user agrees to save input and output.