# web3j java with erc20
* create project
```bash
mvn archetype:generate 	-DgroupId=edu.lab.erc20	-DartifactId=demo-erc20	-DinteractiveMode=false -DarchetypeArtifactId=maven-archetype-quickstart
```


- - - -
web3 install
```
curl -L get.web3j.io | sh && source ~/.web3j/source.sh
```
generate files
```
web3j generate solidity  --abiFile=./data.abi --outputDir=./out/ --package=id.com


