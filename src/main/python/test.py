
import json
import requests

with open ("/Users/maayanlab/OneDrive/speedrichr/src/main/python/testinput.txt", "r") as myfile:
        genelistfile=myfile.read()

with open ("/Users/maayanlab/OneDrive/speedrichr/src/main/webapp/WEB-INF/data/human_genes.tsv", "r") as myfile:
        backgroundgenes=myfile.read()


"https://amp.pharm.mssm.edu/Enrichr/datasetStatistics"




baseurl = "https://amp.pharm.mssm.edu/speedrichr/api/"
baseurl = "http://localhost:8666/speedrichr/api/"

library = "TRANSFAC_and_JASPAR_PWMs"



payload = {
    'background': backgroundgenes,
}

OXENRICHR_URL = baseurl+'addbackground'
response = requests.post(OXENRICHR_URL, data=payload)
data_back = json.loads(response.text)
print(json.dumps(data_back, indent=2))


OXENRICHR_URL = baseurl+'addList'
genes_str = '\n'.join([
    'PHF14', 'RBM3', 'MSL1', 'PHF21A', 'ARL10', 'INSR', 'JADE2', 'P2RX7',
    'LINC00662', 'CCDC101', 'PPM1B', 'KANSL1L', 'CRYZL1', 'ANAPC16', 'TMCC1',
    'CDH8', 'RBM11', 'CNPY2', 'HSPA1L', 'CUL2', 'PLBD2', 'LARP7', 'TECPR2', 
    'ZNF302', 'CUX1', 'MOB2', 'CYTH2', 'SEC22C', 'EIF4E3', 'ROBO2',
    'ADAMTS9-AS2', 'CXXC1', 'LINC01314', 'ATF7'
])

description = 'Example gene list'
payload = {
    'list': (None, genelistfile),
    'description': (None, description)
}

payload = {
    'list': (None, genes_str),
    'description': (None, description)
}


OXENRICHR_URL = baseurl+'addList'
response = requests.post(OXENRICHR_URL, files=payload)
data = json.loads(response.text)
print(json.dumps(data, indent=2))




payload = {
    'userListId': data["userListId"],
    'backgroundid': data_back["backgroundid"],
    'backgroundType': library
}

OXENRICHR_URL = baseurl+'backgroundenrich'
response = requests.post(OXENRICHR_URL, data=payload)
data_ben = json.loads(response.text)
print(json.dumps(data_ben, indent=2))




OXENRICHR_URL = baseurl+'view?userListId=%s'
user_list_id = data["userListId"]
response = requests.get(OXENRICHR_URL % user_list_id)
data2 = json.loads(response.text)
print(json.dumps(data2, indent=2))

OXENRICHR_URL = baseurl+'enrich'
query_string = '?userListId=%s&backgroundType=%s'
user_list_id = data["userListId"]
gene_set_library = library
response = requests.get(
    OXENRICHR_URL + query_string % (user_list_id, gene_set_library)
)

data3 = json.loads(response.text)
print(json.dumps(data3, indent=2, sort_keys=True))




OXENRICHR_URL = baseurl+'enrich'
query_string = '?userListId=%s&backgroundType=%s'
user_list_id = data["userListId"]
gene_set_library = library
response = requests.get(
    OXENRICHR_URL + query_string % (user_list_id, gene_set_library)
)

data3 = json.loads(response.text)
print(json.dumps(data3, indent=2, sort_keys=True))




OXENRICHR_URL = baseurl+'export'
query_string = '?userListId=%s&filename=%s&backgroundType=%s'
user_list_id = data["userListId"]
filename = 'example_enrichment'
gene_set_library = 'ENCODE_TF_ChIP-seq_2015'

url = OXENRICHR_URL + query_string % (user_list_id, filename, gene_set_library)
response = requests.get(url, stream=True)

with open(filename + '.txt', 'wb') as f:
    for chunk in response.iter_content(chunk_size=1024): 
        if chunk:
            f.write(chunk)









genes_str = '\n'.join([
    'PHF14', 'RBM3', 'MSL1', 'PHF21A', 'ARL10', 'INSR', 'JADE2', 'P2RX7',
    'LINC00662', 'CCDC101', 'PPM1B', 'KANSL1L', 'CRYZL1', 'ANAPC16', 'TMCC1',
    'CDH8', 'RBM11', 'CNPY2', 'HSPA1L', 'CUL2', 'PLBD2', 'LARP7', 'TECPR2', 
    'ZNF302', 'CUX1', 'MOB2', 'CYTH2', 'SEC22C', 'EIF4E3', 'ROBO2',
    'ADAMTS9-AS2', 'CXXC1', 'LINC01314', 'ATF7', 'ATP5F1'
])
description = 'Example gene list'
payload = {
    'geneset': genes_str,
    'library': "KEA,ChEA 2016"
}

response = requests.post(SPEEDRICHR_URL, data=payload)
if not response.ok:
    raise Exception('Error analyzing gene list')

data = json.loads(response.text)
print(data)





SPEEDRICHR_URL = 'http://localhost:8666/speedrichr/api/enrich'

genes_str = '\n'.join([
    'PHF14', 'RBM3', 'MSL1', 'PHF21A', 'ARL10', 'INSR', 'JADE2', 'P2RX7',
    'LINC00662', 'CCDC101', 'PPM1B', 'KANSL1L', 'CRYZL1', 'ANAPC16', 'TMCC1',
    'CDH8', 'RBM11', 'CNPY2', 'HSPA1L', 'CUL2', 'PLBD2', 'LARP7', 'TECPR2', 
    'ZNF302', 'CUX1', 'MOB2', 'CYTH2', 'SEC22C', 'EIF4E3', 'ROBO2',
    'ADAMTS9-AS2', 'CXXC1', 'LINC01314', 'ATF7', 'ATP5F1'
])
description = 'Example gene list'
payload = {
    'list': (None, genes_str),
    'description': (None, description)
}

response = requests.post(SPEEDRICHR_URL, files=payload)
if not response.ok:
    raise Exception('Error analyzing gene list')

data = json.loads(response.text)
print(data)





query_string = '?userListId=%s&backgroundType=%s'
user_list_id = 363320
gene_set_library = 'ChEA_2016'
response = requests.get(
    SPEEDRICHR_URL + query_string % (user_list_id, gene_set_library)
 )
if not response.ok:
    raise Exception('Error fetching enrichment results')

data = json.loads(response.text)
print(json.dumps(data, indent=4, sort_keys=True))