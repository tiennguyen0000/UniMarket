package com.example.unimarket.data.service;

import com.example.unimarket.data.model.Report;
import com.example.unimarket.data.service.base.BaseCrudService;

public class ReportService extends BaseCrudService<Report> {
    @Override
    public Long getId(Report item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(Report item, Long id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "reports";
    }

    @Override
    protected Class<Report> getModelClass() {
        return Report.class;
    }
}
