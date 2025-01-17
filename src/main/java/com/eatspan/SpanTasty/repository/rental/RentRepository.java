package com.eatspan.SpanTasty.repository.rental;

import java.util.Date;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eatspan.SpanTasty.entity.account.Member;
import com.eatspan.SpanTasty.entity.rental.Rent;

@Repository
public interface RentRepository extends JpaRepository<Rent, Integer> {
	
	
	@Query(value = "from Rent where"
	        + "(:rentId IS NULL OR rentId = :rentId) AND "
	        + "(:memberId IS NULL OR memberId = :memberId) AND "
	        + "(:restaurantId IS NULL OR restaurantId = :restaurantId) AND "
	        + "(:rentStatus IS NULL OR rentStatus = :rentStatus) AND "
	        + "(:rentDateStart IS NULL OR rentDate >= :rentDateStart) AND "
	        + "(:rentDateEnd IS NULL OR rentDate <= :rentDateEnd)")
	List<Rent> findByCriteria(
			@Param("rentId") Integer rentId,
	        @Param("memberId") Integer memberId,
	        @Param("restaurantId") Integer restaurantId,
	        @Param("rentStatus") Integer rentStatus,
	        @Param("rentDateStart") Date rentDateStart,
	        @Param("rentDateEnd") Date rentDateEnd);
	
	
	@Query(value = "SELECT * FROM rent WHERE due_date = DATEADD(DAY, 1, CAST(GETDATE() AS DATE)) AND return_date IS NULL", nativeQuery = true)
	List<Rent> findByDueDateTomorrow();
	
	
	@Query(value = "SELECT * FROM rent WHERE due_date < current_date() and return_date is null", nativeQuery = true)
	List<Rent> findByOverDueDate();
	
	
	@Query(value = "SELECT rent_id FROM rent", nativeQuery = true)
	List<Integer> findRentIds();
	
	
	@Query(value = "SELECT SUM(rentItem.tableware.tablewareDeposit * rentItem.rentItemQuantity) FROM RentItem rentItem WHERE rentItem.rent.rentId = :rentId")
	Integer countRentDeposit(@Param("rentId") Integer rentId);
	
	
	List<Rent> findByMemberId(Integer memberId);
	
	
	@Query("SELECT m FROM Member m JOIN Rent rent ON m.id = rent.member.id WHERE rent.rentId = :rentId")
	Member findMemberByRentId(@Param("rentId") Integer rentId);
	
	
	@Query("SELECT r " +
		       "FROM Rent r " +
		       "JOIN Restaurant restaurant ON restaurant.restaurantId = r.restaurantId " +
		       "WHERE r.memberId = :memberId")
		List<Rent> findAllByMemberId(@Param("memberId") Integer memberId);
}
