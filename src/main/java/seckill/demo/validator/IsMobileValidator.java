package seckill.demo.validator;
import  javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import seckill.demo.util.ValidatorUtil;

/**
 * 真正用户手机号码检验的工具，会被注解@isMobile所使用
 * 这个类需要实现javax.validation.ConstraintValidator，否则不能被@Constraint参数使用
 */
public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

	private boolean required = false;
	
	public void initialize(IsMobile constraintAnnotation) {
		required = constraintAnnotation.required();
	}

	/**
	 * 用于检验字段是否合法
	 *
	 * @param value   待校验的字段
	 * @param context
	 * @return 字段检验结果
	 */
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if(required) {
			return ValidatorUtil.isMobile(value);
		}else {
			if(StringUtils.isEmpty(value)) {
				return true;
			}else {
				return ValidatorUtil.isMobile(value);
			}
		}
	}

}
