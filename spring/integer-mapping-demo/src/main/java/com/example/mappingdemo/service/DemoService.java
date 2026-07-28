package com.example.mappingdemo.service;

import com.example.mappingdemo.dto.DemoDto;
import java.util.List;

public interface DemoService {
    List<DemoDto> getAllRecords();
    DemoDto saveStandard(Integer intCol, Long bigintCol);
    DemoDto saveJavaWrapAround(Long valueToCast);
    void saveDbIntOverflow(Long value);
    void saveDbBigintOverflow();
}
