# Speedrichr

This is the API backend for Enrichr. It supports faster in memory data structures to take the load of the database instance. 

This file should be extracted into the /src/main/webapp/WEB-INF/data/ folder.

## Building a new image and updating gene list files

The data used by Speedrichr are stored directly in the docker image. To update or add a new GMT file the following steps have to be taken:

1.  Update the file:
    /src/main/webapp/WEB-INF/data/datasetStatistics.json

Add information about the new file in the format of:

    {
        "geneCoverage": number of genes,
        "genesPerTerm": average  number of genes,
        "libraryName": name of gene set (same as GMT file name without file ending),
        "link": url of resource,
        "numTerms": number of gene lists,
        "categoryId": category from "categories" at the end of the file
    }

Example:

    {
        "geneCoverage": 17464,
        "genesPerTerm": 63,
        "libraryName": "DisGeNET",
        "link": "https://www.disgenet.org",
        "numTerms": 9828,
        "categoryId": 7
    }

2.  Add GMT file to folder:
    /src/main/webapp/WEB-INF/data/genelibs

3.  Build new docker image and push to docker hub. Go to /docker folder and execute ./buildContainer.sh
    This will also launch a local version of Speedrichr at port 8666 and can be tested at localhost.

    Example:
    http://localhost:8666/speedrichr/api/datasetStatistics
