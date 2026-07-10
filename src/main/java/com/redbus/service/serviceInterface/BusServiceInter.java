package com.redbus.service.serviceInterface;

import com.redbus.dto.request.BusRequest;
import com.redbus.dto.response.BusSearchResponse;
import com.redbus.entity.Bus;
import com.redbus.exception.RedBusException;

import java.util.List;
import java.util.Map;

public interface BusServiceInter {

    Map<String, String> addBus(BusRequest request) throws RedBusException;

    Map<String, String> updateBus(Long busId, BusRequest request) throws RedBusException;

    Map<String, String> deleteBus(Long busId) throws RedBusException;

    List<BusSearchResponse> searchBuses(String from, String to, String dateStr) throws RedBusException;

    List<Integer> getAvailableSeatNumbers(Long busId) throws RedBusException;

    List<Bus> getAllBuses() throws RedBusException;

}
