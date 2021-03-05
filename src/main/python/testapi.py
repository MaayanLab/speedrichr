import json
import requests
import pandas as pd

ENRICHR_URL = 'http://maayanlab.cloud/Enrichr/addList'
genes_str = '\n'.join([
    'PHF14', 'RBM3', 'MSL1', 'PHF21A', 'ARL10', 'INSR', 'JADE2', 'P2RX7',
    'LINC00662', 'CCDC101', 'PPM1B', 'KANSL1L', 'CRYZL1', 'ANAPC16', 'TMCC1',
    'CDH8', 'RBM11', 'CNPY2', 'HSPA1L', 'CUL2', 'PLBD2', 'LARP7', 'TECPR2', 
    'ZNF302', 'CUX1', 'MOB2', 'CYTH2', 'SEC22C', 'EIF4E3', 'ROBO2',
    'ADAMTS9-AS2', 'CXXC1', 'LINC01314', 'ATF7', 'ATP5F1'
])

with open ("testinput.txt", "r") as myfile:
        genes_str=myfile.read()

description = 'Example gene list'
payload = {
    'list': (None, genes_str),
    'description': (None, description)
}

response = requests.post(ENRICHR_URL, files=payload)
if not response.ok:
    raise Exception('Error analyzing gene list')

data = json.loads(response.text)
print(data)

ENRICHR_URL = 'http://maayanlab.cloud/Enrichr/view?userListId=%s'
user_list_id = data["userListId"]
response = requests.get(ENRICHR_URL % user_list_id)

data = json.loads(response.text)
print(data)

ENRICHR_URL = 'http://maayanlab.cloud/Enrichr/enrich'
query_string = '?userListId=%s&backgroundType=%s'
gene_set_library = 'ChEA_2016'
response = requests.get(
    ENRICHR_URL + query_string % (user_list_id, gene_set_library)
)

data = json.loads(response.text)
print(data)

df = pd.DataFrame()

for i in range(len(data[gene_set_library])):
    print(data[gene_set_library][i])
    df[i] = data[gene_set_library][i]

df = df.transpose()
df.iloc[:, [1,2,6]]


