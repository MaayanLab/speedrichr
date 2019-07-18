import glob
import requests
import json

files = glob.glob("/Volumes/UBUNTU-SERV/EnrichrLibraries/*.txt")

SPEEDRICHR_URL = 'http://localhost:8666/speedrichr/api/uploadgmt'

for i in files[120:150]:
    bname = i.split("/")[4]
    bname = bname.replace(".txt", "")
    print(bname)
    with open (i, "r") as myfile:
        data=myfile.read()
        payload = {
            'user':'lachmann12',
            'pwd':'geheim',
            'category': 'Misc',
            'gmtname': bname,
            'description': "",
            'text': "",
            'gmtcontent': data
        }
        response = requests.post(SPEEDRICHR_URL, data=payload)
        if not response.ok:
            raise Exception('Error analyzing gene list')
        data = json.loads(response.text)
        print(data)


