#!/bin/bash
# Copyright London Stock Exchange Group All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#

CHANNEL_NAME="$1"
: ${CHANNEL_NAME:="mychannel"}
: ${TIMEOUT:="60"}
COUNTER=1
MAX_RETRY=5
ORDERER_CA=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem
PEER0_ORG1_CA=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
PEER0_ORG2_CA=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt
ORDERER_SYSCHAN_ID=e2e-orderer-syschan

CHAINCODE=github.com/hyperledger/fabric/examples/chaincode/go/example02/cmd

echo "Channel name : "$CHANNEL_NAME

verifyResult () {
	if [ $1 -ne 0 ] ; then
		echo "!!!!!!!!!!!!!!! "$2" !!!!!!!!!!!!!!!!"
                echo "================== ERROR !!! FAILED to execute End-2-End Scenario =================="
		echo
   	exit 1
	fi
}

setGlobals () {
	PEER=$1
	ORG=$2
	if [ $ORG -eq 1 ] ; then
		CORE_PEER_LOCALMSPID="Org1MSP"
		CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG1_CA
		CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
		if [ $PEER -eq 0 ]; then
			CORE_PEER_ADDRESS=peer0.org1.example.com:7051
		else
			CORE_PEER_ADDRESS=peer1.org1.example.com:7051
		fi
	elif [ $ORG -eq 3 ] ; then
		CORE_PEER_LOCALMSPID="Org3MSP"
		CORE_PEER_TLS_ROOTCERT_FILE=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
		CORE_PEER_ADDRESS=peer0.org1.example.com:7051
		CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/idemix/idemix-config
		CORE_PEER_LOCALMSPTYPE=idemix
	else
		CORE_PEER_LOCALMSPID="Org2MSP"
		CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG2_CA
		CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp
		if [ $PEER -eq 0 ]; then
			CORE_PEER_ADDRESS=peer0.org2.example.com:7051
		else
			CORE_PEER_ADDRESS=peer1.org2.example.com:7051
		fi
	fi

	env |grep CORE
}

installChaincode () {
	PEER=$1
	ORG=$2
	setGlobals $PEER $ORG

	pwd
#	这个peer chaincode install 命令是在cli容器中执行的，且貌似会自动在-p 跟的路径前加上/opt/gopath/src/
	peer chaincode install -n mycc -v 1.0 -p $CHAINCODE >&log.txt
	res=$?
	cat log.txt
	verifyResult $res "Chaincode installation on peer peer${PEER}.org${ORG} has Failed"
	echo "===================== Chaincode is installed on peer${PEER}.org${ORG} ===================== "
	echo
}

instantiateChaincode () {
	PEER=$1
	ORG=$2
	setGlobals $PEER $ORG
	# while 'peer chaincode' command can get the orderer endpoint from the peer (if join was successful),
	# lets supply it directly as we know it using the "-o" option
	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
		peer chaincode instantiate -o orderer.example.com:7050 -C $CHANNEL_NAME -n mycc -v 1.0 -c '{"Args":["init","a","100","b","200"]}' -P "AND ('Org1MSP.peer','Org2MSP.peer')" >&log.txt
	else
		peer chaincode instantiate -o orderer.example.com:7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n mycc -v 1.0 -c '{"Args":["init","a","100","b","200"]}' -P "AND ('Org1MSP.peer','Org2MSP.peer')" >&log.txt
	fi
	res=$?
	cat log.txt
	verifyResult $res "Chaincode instantiation on peer${PEER}.org${ORG} on channel '$CHANNEL_NAME' failed"
	echo "===================== Chaincode is instantiated on peer${PEER}.org${ORG} on channel '$CHANNEL_NAME' ===================== "
	echo
}

chaincodeQuery () {
	PEER=$1
	ORG=$2
	setGlobals $PEER $ORG
	EXPECTED_RESULT=$3
	echo "===================== Querying on peer${PEER}.org${ORG} on channel '$CHANNEL_NAME'... ===================== "
	local rc=1
	local starttime=$(date +%s)

	# continue to poll
	# we either get a successful response, or reach TIMEOUT
	while test "$(($(date +%s)-starttime))" -lt "$TIMEOUT" -a $rc -ne 0
	do
        	sleep 3
        	echo "Attempting to Query peer${PEER}.org${ORG} ...$(($(date +%s)-starttime)) secs"
        	peer chaincode query -C $CHANNEL_NAME -n mycc -c '{"Args":["query","a"]}' >&log.txt
        	test $? -eq 0 && VALUE=$(cat log.txt | egrep '^[0-9]+$')
        	test "$VALUE" = "$EXPECTED_RESULT" && let rc=0
	done
	echo
	cat log.txt
	if test $rc -eq 0 ; then
		echo "===================== Query successful on peer${PEER}.org${ORG} on channel '$CHANNEL_NAME' ===================== "
    	else
		echo "!!!!!!!!!!!!!!! Query result on peer${PEER}.org${ORG} is INVALID !!!!!!!!!!!!!!!!"
        	echo "================== ERROR !!! FAILED to execute End-2-End Scenario =================="
		echo
		exit 1
  fi
}

# parsePeerConnectionParameters $@
# Helper function that takes the parameters from a chaincode operation
# (e.g. invoke, query, instantiate) and checks for an even number of
# peers and associated org, then sets $PEER_CONN_PARMS and $PEERS
parsePeerConnectionParameters() {
	# check for uneven number of peer and org parameters
	if [ $(( $# % 2 )) -ne 0 ]; then
        	exit 1
	fi

	PEER_CONN_PARMS=""
	PEERS=""
	while [ "$#" -gt 0 ]; do
		PEER="peer$1.org$2"
		PEERS="$PEERS $PEER"
		PEER_CONN_PARMS="$PEER_CONN_PARMS --peerAddresses $PEER.example.com:7051"
		if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "true" ]; then
        		TLSINFO=$(eval echo "--tlsRootCertFiles \$PEER$1_ORG$2_CA")
        		PEER_CONN_PARMS="$PEER_CONN_PARMS $TLSINFO"
    fi
		# shift by two to get the next pair of peer/org parameters
    shift; shift
	done
	# remove leading space for output
	PEERS="$(echo -e "$PEERS" | sed -e 's/^[[:space:]]*//')"
}

# chaincodeInvoke <peer> <org> ...
# Accepts as many peer/org pairs as desired and requests endorsement from each
chaincodeInvoke () {
	parsePeerConnectionParameters $@
	res=$?
	verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

	# while 'peer chaincode' command can get the orderer endpoint from the
	# peer (if join was successful), let's supply it directly as we know
	# it using the "-o" option
	if [ -z "$CORE_PEER_TLS_ENABLED" -o "$CORE_PEER_TLS_ENABLED" = "false" ]; then
		peer chaincode invoke -o orderer.example.com:7050 -C $CHANNEL_NAME -n mycc $PEER_CONN_PARMS -c '{"Args":["invoke","a","b","10"]}' >&log.txt
	else
    peer chaincode invoke -o orderer.example.com:7050  --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n mycc $PEER_CONN_PARMS -c '{"Args":["invoke","a","b","10"]}' >&log.txt
	fi
	res=$?
	cat log.txt
	verifyResult $res "Invoke execution on PEER$PEER failed "
	echo "===================== Invoke transaction successful on $PEERS on channel '$CHANNEL_NAME' ===================== "
	echo
}

# Install chaincode on peer0.org1 and peer0.org2
echo "Installing chaincode on peer0.org1..."
installChaincode 0 1
echo "Install chaincode on peer0.org2..."
installChaincode 0 2

# Instantiate chaincode on peer0.org2
echo "Instantiating chaincode on peer0.org2..."
instantiateChaincode 0 2

# Query on chaincode on peer0.org1
echo "Querying chaincode on peer0.org1..."
chaincodeQuery 0 1 100

# Invoke on chaincode on peer0.org1 and peer0.org2
echo "Sending invoke transaction on peer0.org1 and peer0.org2..."
chaincodeInvoke 0 1 0 2

# Install chaincode on peer1.org2
echo "Installing chaincode on peer1.org2..."
installChaincode 1 2

# Query on chaincode on peer1.org2, check if the result is 90
echo "Querying chaincode on peer1.org2..."
chaincodeQuery 1 2 90

#Query on chaincode on peer1.org3 with idemix MSP type, check if the result is 90
echo "Querying chaincode on peer1.org3..."
chaincodeQuery 1 3 90 # 出错，可暂时忽略，不清楚idemix MSP作用

echo
echo "===================== All GOOD, End-2-End execution completed ===================== "
echo

echo
echo " _____   _   _   ____            _____   ____    _____ "
echo "| ____| | \ | | |  _ \          | ____| |___ \  | ____|"
echo "|  _|   |  \| | | | | |  _____  |  _|     __) | |  _|  "
echo "| |___  | |\  | | |_| | |_____| | |___   / __/  | |___ "
echo "|_____| |_| \_| |____/          |_____| |_____| |_____|"
echo

exit 0
