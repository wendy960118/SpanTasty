package com.eatspan.SpanTasty.entity.reservation;



import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "restaurant_table")
public class RestaurantTable {
	
    @EmbeddedId
    private RestaurantTableId id; // 嵌入的複合主鍵

    @Column(name = "table_status")
    private Integer tableStatus; // 桌子是否可供訂位(1=可, 2=不可)
    
    @ManyToOne //預設(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_id", insertable = false, updatable = false)
    private Restaurant restaurant; // 與 Restaurant 的關聯
 
    @ManyToOne //預設(fetch = FetchType.EAGER)
    @JoinColumn(name = "table_type_id", insertable = false, updatable = false)
    private TableType tableType; // 與 TableType 的關聯
    
    // 10/25 test
//	@JsonIgnore //該屬性不要做JSON序列化避免無線迴圈 //預設lazy
//	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "reserve", cascade = CascadeType.ALL)
//	private List<Reserve> restaurantTables = new ArrayList<Reserve>();
    
    @JsonIgnore
    @ManyToMany(mappedBy = "restaurantTables", fetch = FetchType.LAZY)
    private List<Reserve> reserves = new ArrayList<>();
    
    
	@PrePersist //當物件轉換成persist時先做該方法
	public void onCreate() {
		if(this.tableStatus == null) {
			this.tableStatus = 1; // 餐廳桌位狀態 1=可預訂 2=不可預訂
		}
	}

	
	public RestaurantTable(RestaurantTableId id) {
		this.id = id;
	}
    
	

}
