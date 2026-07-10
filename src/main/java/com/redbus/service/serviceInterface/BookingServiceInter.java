package com.redbus.service.serviceInterface;

import com.redbus.dto.request.CancelTicketRequest;
import com.redbus.dto.request.PassengerRequest;
import com.redbus.dto.request.SeatBlockingRequest;
import com.redbus.dto.response.HistoryResponse;
import com.redbus.dto.response.PassengerResponse;
import com.redbus.dto.response.SeatBlockingResponse;
import com.redbus.exception.RedBusException;

import java.util.List;
import java.util.Map;

public interface BookingServiceInter {

    SeatBlockingResponse blockSeat(SeatBlockingRequest request, Long userId) throws RedBusException;

    PassengerResponse savePassengerDetails(PassengerRequest request, Long userId)throws RedBusException;

    List<HistoryResponse> getHistory(Long userId) throws RedBusException;

    Map<String, String> cancelTicket(CancelTicketRequest request, Long userId) throws RedBusException;


}
