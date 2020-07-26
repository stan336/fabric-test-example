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

//RecordInfo 病例信息
type RecordInfo struct {
	Identity string   `json:"identity"`
	SickName string   `json:"sickName"`
	DrugName []string `json:"drugName"`
}

//ResultData 返回结果
type ResultData struct {
	RecordInfos []RecordInfo `json:"recordInfos"`
}

//InitLedger 初始化
func (t *SmartContract) InitLedger(ctx contractapi.TransactionContextInterface) error {
	return nil
}

//Save 保存一条信息
func (t *SmartContract) Save(ctx contractapi.TransactionContextInterface, key string, sickName string, drugName []string, identity string) error {

	recordInfo := RecordInfo{
		Identity: identity,
		SickName: sickName,
		DrugName: drugName,
	}

	recordInfoAsBytes, err := json.Marshal(recordInfo)
	if err != nil {
		return err
	}
	return ctx.GetStub().PutState(key, recordInfoAsBytes)
}

//QueryRecord 查询一条基本信息
func (t *SmartContract) QueryRecord(ctx contractapi.TransactionContextInterface, key string) (*RecordInfo, error) {
	recordInfoAsBytes, err := ctx.GetStub().GetState(key)
	if err != nil {
		return nil, fmt.Errorf("Failed to read from world state. %s", err.Error())
	}

	if recordInfoAsBytes == nil {
		return nil, fmt.Errorf("%s does not exist", key)
	}

	recordInfo := new(RecordInfo)
	_ = json.Unmarshal(recordInfoAsBytes, recordInfo)
	return recordInfo, nil
}

// QueryHistoryRecord 获取一个key的历史修改记录，用于测试合约升级功能，升级就是发布新版本
func (t *SmartContract) QueryHistoryRecord(ctx contractapi.TransactionContextInterface, key string) (*ResultData, error) {
	resultsIterator, err := ctx.GetStub().GetHistoryForKey(key)

	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	recordInfos := []RecordInfo{}

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()

		if err != nil {
			return nil, err
		}

		recordInfo := new(RecordInfo)
		_ = json.Unmarshal(queryResponse.Value, recordInfo)

		recordInfos = append(recordInfos, *recordInfo)
	}
	resultData := &ResultData{}
	resultData.RecordInfos = recordInfos

	return resultData, nil
}

func main() {
	chaincode, err := contractapi.NewChaincode(new(SmartContract))
	if err != nil {
		fmt.Println("Error create recordInfo chaincode")
	}

	if err := chaincode.Start(); err != nil {
		fmt.Printf("Error starting recordInfo chaincode: %s", err.Error())
	}
}
