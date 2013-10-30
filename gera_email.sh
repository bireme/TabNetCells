mv current.txt last.txt
curl http://www.datasus.gov.br/idb | grep -o idb.... > current.txt
diff current.txt last.txt | wc -m
