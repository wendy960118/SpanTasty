package com.eatspan.SpanTasty.dto.rental;




import lombok.Data;

@Data
public class RentItemDTO {
	
    private Integer rentId;
    private Integer tablewareId;
    private Integer rentItemQuantity;
    private Integer rentItemDeposit;
    private Integer returnStatus;
    private String returnMemo;
    private String tablewareName;
    
    private Integer returnedQuantity;
    private Integer damagedQuantity;
    
    public RentItemDTO(Integer rentId, Integer tablewareId, Integer rentItemQuantity, Integer rentItemDeposit,
            String returnMemo, Integer returnStatus, String tablewareName, Integer returnedQuantity, Integer damagedQuantity) {
		this.rentId = rentId;
		this.tablewareId = tablewareId;
		this.rentItemQuantity = rentItemQuantity;
		this.rentItemDeposit = rentItemDeposit;
		this.returnMemo = returnMemo;
		this.returnStatus = returnStatus;
		this.returnedQuantity = returnedQuantity;
		this.damagedQuantity = damagedQuantity;
		this.tablewareName = tablewareName;
	}
}
