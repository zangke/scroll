package andy.book

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * 反射工具类.
 */
internal object ReflectUtil {
    fun setFieldValue(obj: Any, fieldName: String, value: Any) {
        val field = getAccessibleField(obj, fieldName) ?: throw IllegalArgumentException("Could not find field [$fieldName] on target [$obj]")

        try {
            field.set(obj, value)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

    }

    fun getAccessibleField(obj: Any?, fieldName: String?): Field? {
        if (obj == null) {
            throw IllegalArgumentException("object can't be null")
        }

        if (fieldName == null || fieldName.isEmpty()) {
            throw IllegalArgumentException("fieldName can't be blank")
        }

        var superClass: Class<*> = obj.javaClass
        while (superClass != Any::class.java) {
            try {
                val field = superClass.getDeclaredField(fieldName)
                makeAccessible(field)
                return field
            } catch (e: NoSuchFieldException) {
                superClass = superClass.superclass
                continue
            }

            superClass = superClass.superclass
        }
        return null
    }

    fun makeAccessible(field: Field) {
        if ((!Modifier.isPublic(field.modifiers) || !Modifier.isPublic(field.declaringClass.modifiers) || Modifier.isFinal(field.modifiers)) && !field.isAccessible) {
            field.isAccessible = true
        }
    }
}
