package main

import (
	"encoding/json"
	"fmt"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

//SmartContract 提供方法用来管理basicInfo
type SmartContract struct {
	contractapi.Contract
}

//BasicInfo 患者基本信息
type BasicInfo struct {
	Identity string `json:"identity"`
	Name     string `json:"name"`
	Mobile   string `json:"mobile"`
}

//InitLedger 初始化
func (t *SmartContract) InitLedger(ctx contractapi.TransactionContextInterface) error {
	return nil
}

//Save 保存一条患者基本信息
func (t *SmartContract) Save(ctx contractapi.TransactionContextInterface, key string, name string, mobile string, identity string) error {
	basicInfo := BasicInfo{
		Identity: identity,
		Name:     name,
		Mobile:   mobile,
	}

	basicInfoAsBytes, err := json.Marshal(basicInfo)
	if err != nil {
		return err
	}
	return ctx.GetStub().PutState(key, basicInfoAsBytes)
}

//QueryInfo 查询一条患者基本信息
func (t *SmartContract) QueryInfo(ctx contractapi.TransactionContextInterface, key string) (*BasicInfo, error) {
	basicInfoAsBytes, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("Failed to read from world state. %s", err.Error())
	}

	if basicInfoAsBytes == nil {
		return nil, fmt.Errorf("%s does not exist", key)
	}

	basicInfo := new(BasicInfo)
	_ = json.Unmarshal(basicInfoAsBytes, basicInfo)
	return basicInfo, nil
}

func main() {
	chaincode, err := contractapi.NewChaincode(new(SmartContract))
	if err != nil {
		fmt.Println("Error create basincInfo chaincode")
	}

	if err := chaincode.Start(); err != nil {
		fmt.Printf("Error starting basincInfo chaincode: %s", err.Error())
	}
}
