package com.eatspan.SpanTasty.entity.rental;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.eatspan.SpanTasty.entity.account.Member;
import com.eatspan.SpanTasty.entity.reservation.Restaurant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rent")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class Rent implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id@Column(name = "rent_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer rentId;
	
	@Column(name = "rent_deposit")
	private Integer rentDeposit;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Column(name = "rent_date")
	@Temporal(TemporalType.DATE)
	private Date rentDate;
	
	@Column(name = "restaurant_id")
	private Integer restaurantId;
	
	@Column(name = "member_id", insertable = true, updatable = false)
	private Integer memberId;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Column(name = "due_date")
	@Temporal(TemporalType.DATE)
	private Date dueDate;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Column(name = "return_date")
	@Temporal(TemporalType.DATE)
	private Date returnDate;
	
	@Column(name = "rent_status")
	private Integer rentStatus;
	
	@Column(name = "rent_memo")
	private String rentMemo;
	
	@Column(name = "return_restaurant_id")
	private Integer returnRestaurantId;
	
	
	@JsonIgnore
	@OneToMany(mappedBy = "rent" , fetch = FetchType.EAGER)
	private List<RentItem> rentItems = new ArrayList<RentItem>();
	
	
	@JsonIgnore
	@ManyToOne
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", insertable = false, updatable = false)  // 外鍵
    private Member member;
	
	
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "restaurant_id", referencedColumnName ="restaurant_id", insertable = false, updatable = false)  // 外鍵
	private Restaurant restaurant;
}
