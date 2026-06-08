# How to build and install BLANT Prediction
 
Core dependencies: `Maven`, `jdk`

## 1. Install JDK

### 1-1. Install JDK in macOS

```
brew install openjdk
```

### 1-2. Install JDK in Linux (Ubuntu/Debian)

```
sudo apt update
sudo apt install openjdk-jdk
```

### 1-2. Install JDK in Windows

- by using Chocolatey
```
choco install microsoft-openjdk
```

- manual install

Go to https://www.oracle.com/java/technologies/downloads/ and download Installer to install.

## 2. Install Apache Maven

### 1-1. Install Apache Maven in macOS

```
brew install maven
```

### 1-2. Install Apache Maven in Linux (Ubuntu/Debian)

```
sudo apt update
sudo apt install maven
```

### 1-2. Install Apache Maven in Windows

- by using Chocolatey
```
choco install maven
```

- manual install

1. Go to https://maven.apache.org/download.cgi and download binary file of `maven`
2. Unzip downloaded binary file in desired directory
3. Add these system environment path for the binary file
- `MAVEN_HOME`: The directory where maven is unzipped
- `Path`: Add `%MAVEN_HOME%\bin`

## 3. Build application using Maven

```
mvn clean install
```
This commend will build `.jar` file in `./target` folder.

## 4. Move the .jar file into Cytoscape Configuration folder.

Your Cytoscape Configuration folder is usually located in your home directory. Locate `CytoscapeConfiguration`, go to `.\3\apps\installed`, and move jar file there. 

Cytoscape will automatically install BLANT Prediction into your Cytoscape.

### Note
BLANT Prediction will also save user input/output caches in `CytoscapeConfiguration\3\apps\BLANT` after running the task if the user agrees to save input and output.