package com.effourt.calenkit.repository;

import com.effourt.calenkit.domain.Schedule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;


public interface ScheduleRepository {
    Integer save(String date);
    Integer update(Schedule schedule);
    Integer updateStatus(Schedule schedule);
    Integer delete(Integer scNo);
    Integer findLastInsertScNo();
    Schedule findByScNo(Integer scNo);
    List<Schedule> findAllByScNo(Map<String, Object> map);
    List<Schedule> findByRecycleBin(Map<String, Object> map);
    List<Schedule> findByFilter(Map<String, Object> map);

    Integer findByRecycleBinCount(Map<String, Object> map);
    Integer findByFilterCount(Map<String, Object> map);
}
