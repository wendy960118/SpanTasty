package com.eatspan.SpanTasty.service.order.Impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatspan.SpanTasty.entity.order.TogoEntity;
import com.eatspan.SpanTasty.repository.order.TogoRepository;
import com.eatspan.SpanTasty.service.order.TogoService;

@Service
public class TogoServiceImpl implements TogoService {
	
	@Autowired
	private TogoRepository togoRepository;
	
	@Override
	public List<TogoEntity> getAllTogo() {
		return togoRepository.findAll();
	}
	
	@Override
	public Page<TogoEntity> getAllTogoPage(Integer pageNumber, Integer itemNumber) {
		if(itemNumber == null) {
			itemNumber=10;
		}
		Pageable pageable = PageRequest.of(pageNumber-1, itemNumber, Sort.Direction.DESC, "togoId");
		return togoRepository.findAll(pageable);
	}
	
	@Override
	public TogoEntity getTogoById(Integer togoId) {
		Optional<TogoEntity> optional = togoRepository.findById(togoId);
		if (optional.isPresent()) {
			return optional.get();
		}
		return null;
	}

	@Override
	public List<TogoEntity> getTogoByMemberId(Integer memberId) {
		return togoRepository.findByMemberIdOrderByTogoIdDesc(memberId);
	}

	@Override
	public List<TogoEntity> getTogoByPhone(String tgPhone) {
		return togoRepository.findTogoByTgPhoneContainingOrderByTogoIdDesc(tgPhone);
	}
	
	@Override
	public List<TogoEntity> getTogoByTgNameAndTgPhone(String tgName, String tgPhone) {
		return togoRepository.findByTgNameAndTgPhone(tgName, tgPhone);
	}
	
	@Override
	public  Page<TogoEntity> getTogoByRestaurantId(Integer pageNumber, Integer itemNumber, Integer restaurantId) {
		if(itemNumber == null) {
			itemNumber=10;
		}
		Pageable pageable = PageRequest.of(pageNumber-1, itemNumber, Sort.Direction.DESC, "togoId");
		return togoRepository.findByRestaurantId(restaurantId, pageable);
	}

	@Override
	public TogoEntity addTogo(TogoEntity newtogo) {
		try {
			TogoEntity savedTogo = togoRepository.save(newtogo);
			return savedTogo;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Transactional
	@Override
	public TogoEntity updateTogoById(Integer togoId, TogoEntity updateTogo) {
		Optional<TogoEntity> optional = togoRepository.findById(togoId);
		if (optional.isPresent()) {
			TogoEntity togo = optional.get();
			if (updateTogo.getMemberId() != null) {
				togo.setMemberId(updateTogo.getMemberId());
			}
			if (updateTogo.getTgName() != null) {
				togo.setTgName(updateTogo.getTgName());
			}
			if (updateTogo.getTgPhone() != null) {
				togo.setTgPhone(updateTogo.getTgPhone());
			}
			if (updateTogo.getRentId() != null) {
				togo.setRentId(updateTogo.getRentId());
			}
			if (updateTogo.getTogoStatus() != null) {
				togo.setTogoStatus(updateTogo.getTogoStatus());
			}
			if (updateTogo.getTogoMemo() != null) {
				togo.setTogoMemo(updateTogo.getTogoMemo());
			}
			return togo;
		}
		return null;
	}
	
	@Transactional
	@Override
	public TogoEntity deleteTogoById(Integer togoId) {
		Optional<TogoEntity> optional = togoRepository.findById(togoId);
		TogoEntity togo = optional.get();
		togo.setTogoStatus(3);
		return togo;
	}

	

}
