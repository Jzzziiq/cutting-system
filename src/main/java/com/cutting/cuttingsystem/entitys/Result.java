package com.cutting.cuttingsystem.entitys;

import lombok.Data;

@Data
public class Result {
    private Integer code;
    private String msg;
    private Object data;


    // 删除插入这种没有响应数据的情况
    public  static  Result success() {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("success");
        return result;
    }
    // 插入、更新、删除这种有响应数据
    public static Result success(Object data) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    // 错误响应
    public static Result error(String msg) {
        Result result = new Result();
        result.setCode(0);
        result.setMsg(msg);
        return result;
    }

}
