package com.example.mappingdemo.service.impl;

import com.example.mappingdemo.dto.DemoDto;
import com.example.mappingdemo.entity.DemoEntity;
import com.example.mappingdemo.repository.DemoRepository;
import com.example.mappingdemo.service.DemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoServiceImpl implements DemoService {

    private final DemoRepository demoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DemoDto> getAllRecords() {
        return demoRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DemoDto saveStandard(Integer intCol, Long bigintCol) {
        log.info("Saving standard values - intCol: {}, bigintCol: {}", intCol, bigintCol);
        DemoEntity entity = new DemoEntity();
        entity.setIntCol(intCol);
        entity.setBigintCol(bigintCol);
        DemoEntity saved = demoRepository.save(entity);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public DemoDto saveJavaWrapAround(Long valueToCast) {
        log.info("Demonstrating Java casting wrap-around for value: {}", valueToCast);
        
        // Java casting wrap-around:
        // 2,147,483,648L (Long) is binary: 00000000 00000000 00000000 00000000 10000000 00000000 00000000 00000000
        // Casting to 32-bit int truncates it to: 10000000 00000000 00000000 00000000 which is -2,147,483,648.
        int javaCastedValue = valueToCast.intValue();
        log.info("Java cast result (Long to int): {}", javaCastedValue);

        DemoEntity entity = new DemoEntity();
        entity.setIntCol(javaCastedValue);
        entity.setBigintCol(valueToCast);
        DemoEntity saved = demoRepository.save(entity);
        
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public void saveDbIntOverflow(Long value) {
        log.info("Triggering DB INT overflow by inserting value: {} into standard INT column", value);
        // We pass the Long value directly via native query to Postgres to bypass Java's 4-byte check
        demoRepository.insertNative(value, 0L);
    }

    @Override
    @Transactional
    public void saveDbBigintOverflow() {
        log.info("Triggering DB BIGINT overflow by inserting literal exceeding 8-byte range");
        // Trigger SQL state 22003 (bigint out of range) via native insert
        demoRepository.insertBigintOverflowNative();
    }

    private DemoDto convertToDto(DemoEntity entity) {
        return DemoDto.builder()
                .id(entity.getId())
                .intCol(entity.getIntCol())
                .bigintCol(entity.getBigintCol())
                .build();
    }
}
