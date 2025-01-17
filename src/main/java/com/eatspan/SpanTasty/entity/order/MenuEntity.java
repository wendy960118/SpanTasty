package com.eatspan.SpanTasty.entity.order;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "menu")
public class MenuEntity implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id @Column(name = "food_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer foodId;
	
	@Column(name = "food_name")
	private String foodName;
	
	@Column(name = "food_picture")
	private String foodPicture;
	
	@Column(name = "food_price")
	private Integer foodPrice;
	
	@Column(name = "food_kind_id" ,insertable = false, updatable = false)
	private Integer foodKindId;
	
	@Column(name = "food_stock")
	private Integer foodStock;
	
	@Column(name = "food_description")
	private String foodDescription;
	
	@Column(name = "food_status")
	private Integer foodStatus;

	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "food_kind_id")
	private FoodKindEntity foodKind;
	
	@JsonIgnore
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "menu")
	private List<TogoItemEntity> togoItem = new ArrayList<TogoItemEntity>();
	
	@PrePersist
	public void defaultData() {
		if(this.foodStatus == null) {
			this.foodStatus = 1;
		}
	}

}
