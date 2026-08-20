package com.splitlink.mapper;

import com.splitlink.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomMapper {

    void insertRoom(Room room);

    void insertMembers(@Param("roomId") Long roomId, @Param("memberNames") List<String> memberNames);
}
