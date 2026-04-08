package com.example.unimarket.data.service;

import com.example.unimarket.data.model.StudentVerification;
import com.example.unimarket.data.service.base.BaseCrudService;

public class StudentVerificationService extends BaseCrudService<StudentVerification> {
    @Override
    public Long getId(StudentVerification item) {
        return item != null ? item.getId() : null;
    }

    @Override
    public void setId(StudentVerification item, Long id) {
        if (item != null) {
            item.setId(id);
        }
    }

    @Override
    protected String getTableName() {
        return "student_verifications";
    }

    @Override
    protected Class<StudentVerification> getModelClass() {
        return StudentVerification.class;
    }
}
