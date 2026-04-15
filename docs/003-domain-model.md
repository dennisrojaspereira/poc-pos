
# Domain Model

Transaction:
- transactionId
- terminalId
- nsu
- amount
- status

Rules:
- unique terminalId + nsu
- unique transactionId
- transitions válidas
