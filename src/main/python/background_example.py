
import json
import requests


baseurl = "https://amp.pharm.mssm.edu/speedrichr/api/"


# now we have to upload a gene set that we want to calculate enrichment for
# the function returns an identifier which we can refer to in later calls (userListId)
with open ("testinput.txt", "r") as myfile:
        genelist=myfile.read()

OXENRICHR_URL = baseurl+'addList'
description = 'Example gene list'
payload = {
    'list': (None, genelist),
    'description': (None, description)
}
response = requests.post(OXENRICHR_URL, files=payload)
data = json.loads(response.text)
print(json.dumps(data, indent=2))


# now we upload the background we want to use in this case
# the function returns an identifier which we can refer to in later calls (backgroundid)
with open ("human_genes.tsv", "r") as myfile:
        backgroundgenes=myfile.read()

payload = {
    'background': backgroundgenes,
}
OXENRICHR_URL = baseurl+'addbackground'
response = requests.post(OXENRICHR_URL, data=payload)
data_back = json.loads(response.text)
print(json.dumps(data_back, indent=2))



# list available libraries
OXENRICHR_URL = baseurl+'listlibs'
response = requests.get(OXENRICHR_URL)
data_libs = json.loads(response.text)
print(json.dumps(data_libs, indent=2))


# here we select a library and apply enrichment using the two previously uploaded gene sets
library = "GO_Biological_Process_2018"

payload = {
    'userListId': data["userListId"],
    'backgroundid': data_back["backgroundid"],
    'backgroundType': library
}

OXENRICHR_URL = baseurl+'backgroundenrich'
response = requests.post(OXENRICHR_URL, data=payload)
data_ben = json.loads(response.text)
print(json.dumps(data_ben, indent=2))


# The results will look something like this
# First value is the rank based on p-value, odds ratio, combined score (-log(p)*odds), list of overlapping genes, FDR, 0, 0

{
  "GO_Biological_Process_2018": [
    [
      1, 
      "mitochondrion organization (GO:0007005)", 
      0.0002911735627537808, 
      4.243168604651163, 
      34.546143463176556, 
      [
        "DNAJC19", 
        "YME1L1", 
        "TFAM", 
        "TIMM44", 
        "MTFR1", 
        "POLRMT", 
        "TFB1M", 
        "ATAD3A", 
        "ATPAF1"
      ], 
      0.04658777004060492, 
      0, 
      0
    ], 
    [
      2, 
      "alpha-amino acid metabolic process (GO:1901605)", 
      0.0007162888403260705, 
      9.733433358339585, 
      70.48394774744325, 
      [
        "SRR", 
        "ALDH6A1", 
        "KMO", 
        "MUT"
      ], 
      0.011102477025054092, 
      0, 
      0
    ], 
