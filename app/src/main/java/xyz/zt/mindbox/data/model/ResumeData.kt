package xyz.zt.mindbox.data.model

import android.net.Uri

/**
 * Modelo principal que agrupa toda la información del currículum.
 */
data class ResumeData(
    val personalInfo: PersonalInfo = PersonalInfo(),
    val experiences: List<Experience> = emptyList(),
    val education: Education = Education(),
    val languages: List<Language> = emptyList(),
    val skills: String = "",
    val additionalInfo: String = "",
    val references: List<Reference> = emptyList(),
    val photoUri: Uri? = null
)

/**
 * Información de contacto y datos personales básicos.
 */
data class PersonalInfo(
    val name: String = "",
    val birthDate: String = "",
    val gender: String = "Masculino",
    val maritalStatus: String = "Soltero",
    val professionalId: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = ""
)

/**
 * Representa un bloque de experiencia laboral.
 */
data class Experience(
    val company: String = "",
    val position: String = "",
    val period: String = "",
    val description: String = ""
)

/**
 * Grados académicos principales.
 */
data class Education(
    val university: String = "",
    val postgraduate: String = "",
    val secondary: String = ""
)

/**
 * Idiomas y nivel de dominio.
 */
data class Language(
    val name: String = "",
    val level: String = "Básico"
)

/**
 * Referencias laborales o personales.
 */
data class Reference(
    val name: String = "",
    val email: String = "",
    val company: String = "",
    val phone: String = ""
)