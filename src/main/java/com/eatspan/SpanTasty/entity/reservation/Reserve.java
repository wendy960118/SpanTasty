package com.eatspan.SpanTasty.entity.reservation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.eatspan.SpanTasty.entity.account.Member;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@NoArgsConstructor
@Entity @Table(name = "reserve")
public class Reserve {
	
	
	@Id @Column(name = "reserve_id") @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer reserveId;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "UTC+8")
	@Column(name = "reserve_create_time")
	private LocalDateTime reserveCreateTime;
	
	@Column(name = "reserve_seat")
	private Integer reserveSeat;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "UTC+8")
	@Column(name = "reserve_time")
	private LocalDateTime reserveTime;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "UTC+8")
	@Column(name = "finished_time")
	private LocalDateTime finishedTime;
	
	@Column(name = "reserve_status")
	private Integer reserveStatus;
	
	@Column(name = "table_id")
	private String tableId;
	
	@Column(name = "reserve_note")
	private String reserveNote;
	
	@ManyToOne //預設(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_id")
	//@JoinColumn(name = "restaurant_id", insertable = false, updatable = false)
    private Restaurant restaurant; // 與 Restaurant 的關聯
	
	@ManyToOne //預設(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id")
	//@JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Member member; // 與 Member 的關聯	
	
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "reserve_table",
        joinColumns = @JoinColumn(name = "reserve_id"),
        inverseJoinColumns = {
            @JoinColumn(name = "table_id"),
            @JoinColumn(name = "restaurant_id"),
            @JoinColumn(name = "table_type_id"),
        }
    )
    private List<RestaurantTable> restaurantTables = new ArrayList<>();
	
	
	@PrePersist //當物件轉換成persist時先做該方法
	public void onCreate() {
		if(this.reserveCreateTime == null) {
			this.reserveCreateTime = LocalDateTime.now(); // 使用 LocalDateTime.now() 設置當前時間
		}
		if(this.reserveStatus == null) {
			this.reserveStatus = 1; // 1=未完成, 2=已完成, 3=已取消
		}
	}
	
	
	 public String getFormattedReserveTime() {
	     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	     return reserveTime != null ? reserveTime.format(formatter) : null;
	 }


	@Override
	public String toString() {
		return "Reserve [reserveId=" + reserveId + ", reserveCreateTime=" + reserveCreateTime + ", reserveSeat="
				+ reserveSeat + ", reserveTime=" + reserveTime + ", finishedTime=" + finishedTime + ", reserveStatus="
				+ reserveStatus + ", reserveNote=" + reserveNote + ", restaurantTables=" + restaurantTables + "]";
	}
	
	

}
