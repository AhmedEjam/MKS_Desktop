package com.ahmedyejam.mks.data.seeder

import com.ahmedyejam.mks.data.local.entity.BlueprintMode
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import com.ahmedyejam.mks.data.repository.BookRepository
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MksDatabaseSeeder
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        private val quizRepository: QuizRepository,
        private val knowledgeRepository: KnowledgeRepository,
    ) {
        suspend fun seedDatabase(workspaceId: Long) {
            val books = bookRepository.getAllBooks().first()
            if (books.any { it.externalId == "book_general_knowledge" }) {
                return
            }

            val bookId =
                bookRepository.insertBook(
                    BookEntity(
                        workspaceId = workspaceId,
                        externalId = "book_general_knowledge",
                        title = "General Knowledge & معلومات عامة",
                        description = "A sample bilingual book with an English and Arabic quiz.",
                        iconName = "🌍",
                    ),
                )

            // --- English Quiz ---
            val englishQuizId =
                quizRepository.insertQuiz(
                    QuizEntity(
                        externalId = "5cfcd20f-b26d-46a5-86a3-1576bdc83ff7",
                        bookId = bookId,
                        title = "Sample quiz",
                        description = "25 General Knowledge Questions",
                        iconName = "🇺🇸",
                        category = "General",
                    ),
                )

            quizRepository.insertQuestions(
                listOf(
                    QuestionEntity(
                        externalId = "166e05fb-147e-4978-880e-97c4a6de8a88",
                        quizId = englishQuizId,
                        text = "Which planet is known as the Red Planet?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Jupiter", "Mars", "Venus", "Saturn"),
                        correctAnswers = listOf(1), // Mars
                        explanation = "Its surface is covered in iron oxide.",
                        hint = "It's the 4th planet from the sun.",
                        categories = listOf("Samples"),
                        additionalInfo = "It's the 4th planet from the sun.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "5b30ec00-0f86-4310-9aff-14719c81a11d",
                        quizId = englishQuizId,
                        text = "What is the core component of a Linux operating system called?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("GUI", "Kernel", "Shell", "Terminal"),
                        correctAnswers = listOf(1), // Kernel
                        explanation = "It manages the system's resources completely.",
                        hint = "Starts with the letter 'K'.",
                        categories = listOf("Samples"),
                        additionalInfo = "Starts with the letter 'K'.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "72d2156b-4e74-430e-8ef8-f0bc4ad31727",
                        quizId = englishQuizId,
                        text = "This ancient blue-glazed brick gate was the main entrance to which city?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Sumer", "Babylon", "Ur", "Nineveh"),
                        correctAnswers = listOf(1), // Babylon
                        explanation = "Known as the Ishtar Gate in ancient Iraq.",
                        hint = "Near the city of Hillah.",
                        imagePath = "https://images.unsplash.com/photo-1599395232742-1e944b207238?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "Near the city of Hillah.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "6092f197-f30e-49bb-bb4d-e99bcabc3930",
                        quizId = englishQuizId,
                        text = "What two power sources are used in a Hybrid Electric Vehicle (HEV)?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Internal combustion & Electric", "Solar & Electric", "Hydrogen & Diesel", "Electric & Wind"),
                        correctAnswers = listOf(0), // Internal combustion & Electric
                        explanation = "Uses both fuel and a battery to provide power.",
                        hint = "It is a mix of two systems.",
                        categories = listOf("Samples"),
                        additionalInfo = "It is a mix of two systems.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "e7ffd9c7-4842-49bf-8ce8-e003174eb054",
                        quizId = englishQuizId,
                        text = "Who wrote the famous dystopian novel '1984'?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Ernest Hemingway", "George Orwell", "Mark Twain", "Charles Dickens"),
                        correctAnswers = listOf(1), // George Orwell
                        explanation = "A famous novel discussing totalitarianism.",
                        hint = "'Big Brother is watching you.'",
                        categories = listOf("Samples"),
                        additionalInfo = "'Big Brother is watching you.'",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "39e6d2ad-6bb5-4604-bd25-de1bccb33e2c",
                        quizId = englishQuizId,
                        text = "What display technology is commonly used in modern foldables?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("LCD", "CRT", "OLED", "Plasma"),
                        correctAnswers = listOf(2), // OLED
                        explanation = "Relies on flexible organic layers that can bend.",
                        hint = "Emits its own light without a backlight.",
                        imagePath = "https://images.unsplash.com/photo-1585338107529-13afc5f02586?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "Emits its own light without a backlight.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "e2a16599-1806-447d-b553-7fb0a59369c9",
                        quizId = englishQuizId,
                        text = "How many chambers does a normal human heart have?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Two", "Three", "Four", "Five"),
                        correctAnswers = listOf(2), // Four
                        explanation = "Consists of two atria and two ventricles.",
                        hint = "Two on each side.",
                        imagePath = "https://images.unsplash.com/photo-1530026405186-ed1f139313f8?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "Two on each side.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "b4f4fff6-4354-4bae-863e-140ab8078b6b",
                        quizId = englishQuizId,
                        text = "Which of the following cities is the capital of Japan?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Seoul", "Beijing", "Tokyo", "Bangkok"),
                        correctAnswers = listOf(2), // Tokyo
                        explanation = "It is the largest metropolitan area in the world.",
                        hint = "Hosted the 2020 Summer Olympics.",
                        categories = listOf("Samples"),
                        additionalInfo = "Hosted the 2020 Summer Olympics.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "a7d6cdc7-d80b-4d4b-9e9f-e6f1ad3e4d2c",
                        quizId = englishQuizId,
                        text = "'Masgouf' is a traditional dish famously associated with which country?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Egypt", "Iraq", "Lebanon", "Morocco"),
                        correctAnswers = listOf(1), // Iraq
                        explanation = "Consists of seasoned, grilled carp.",
                        hint = "Famous along Abu Nuwas street.",
                        imagePath = "https://images.unsplash.com/photo-1628174542718-f2b74052345d?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "Famous along Abu Nuwas street.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "45561625-b716-4bd3-a64d-4fb0467b2089",
                        quizId = englishQuizId,
                        text = "What energy source generates electricity using photovoltaic cells?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Wind", "Geothermal", "Solar", "Hydro"),
                        correctAnswers = listOf(2), // Solar
                        explanation = "Extracts energy directly from sunlight.",
                        hint = "Comes from the sky during the day.",
                        categories = listOf("Samples"),
                        additionalInfo = "Comes from the sky during the day.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "63a59436-48eb-45ae-9244-35762cc5ada5",
                        quizId = englishQuizId,
                        text = "What does the acronym VPN stand for?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Visual Private", "Virtual Public", "Virtual Private", "Variable Protocol"),
                        correctAnswers = listOf(2), // Virtual Private
                        explanation = "Encrypts internet traffic to protect data.",
                        hint = "The first word is 'Virtual'.",
                        categories = listOf("Samples"),
                        additionalInfo = "The first word is 'Virtual'.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "7374c867-a464-4ca3-8a00-b1e5b43594fb",
                        quizId = englishQuizId,
                        text = "What is the recommended CPR compression-to-breath ratio for adults?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("15:2", "30:2", "10:1", "50:5"),
                        correctAnswers = listOf(1), // 30:2
                        explanation = "The ratio recommended by the AHA.",
                        hint = "Starts with 30 compressions.",
                        categories = listOf("Samples"),
                        additionalInfo = "Starts with 30 compressions.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "09e8e0c4-0f6e-4e49-bb9a-d9f335646fef",
                        quizId = englishQuizId,
                        text = "Who formulated the laws of motion and universal gravitation?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Albert Einstein", "Isaac Newton", "Nikola Tesla", "Galileo Galilei"),
                        correctAnswers = listOf(1), // Isaac Newton
                        explanation = "Known for the famous apple story.",
                        hint = "He has 3 fundamental laws of motion.",
                        categories = listOf("Samples"),
                        additionalInfo = "He has 3 fundamental laws of motion.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "97dbe833-2787-408d-b578-4a3235b14f95",
                        quizId = englishQuizId,
                        text = "Which programming language is Google's preferred choice for Android?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Java", "Swift", "Kotlin", "C++"),
                        correctAnswers = listOf(2), // Kotlin
                        explanation = "A modern, expressive, and concise language.",
                        hint = "Created by JetBrains.",
                        imagePath = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "Created by JetBrains.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "ce4d8b5d-0aea-43e4-b98e-d29c33c94097",
                        quizId = englishQuizId,
                        text = "Who is credited with inventing the telephone?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Thomas Edison", "Alexander Graham Bell", "Guglielmo Marconi", "James Watt"),
                        correctAnswers = listOf(1), // Alexander Graham Bell
                        explanation = "Patented the first practical telephone.",
                        hint = "Alexander Graham...",
                        categories = listOf("Samples"),
                        additionalInfo = "Alexander Graham...",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "da55b633-7bb1-4bf8-a085-8c2eeb8a0b0f",
                        quizId = englishQuizId,
                        text = "Which of the following is a fast-acting IV anesthetic?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Paracetamol", "Propofol", "Aspirin", "Ibuprofen"),
                        correctAnswers = listOf(1), // Propofol
                        explanation = "Widely used for the induction of anesthesia.",
                        hint = "Nicknamed the 'Milk of Amnesia'.",
                        categories = listOf("Samples"),
                        additionalInfo = "Nicknamed the 'Milk of Amnesia'.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "3aa7d740-58a6-424c-9eed-13513f59396c",
                        quizId = englishQuizId,
                        text = "Who painted the famous Mona Lisa?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Vincent van Gogh", "Pablo Picasso", "Leonardo da Vinci", "Claude Monet"),
                        correctAnswers = listOf(2), // Leonardo da Vinci
                        explanation = "An Italian Renaissance polymath.",
                        hint = "Leonardo...",
                        imagePath = "https://images.unsplash.com/photo-1577083165350-16c97a8b694f?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "Leonardo...",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "863fdd73-6352-4e73-8a8e-b21bb957aaa7",
                        quizId = englishQuizId,
                        text = "What is the chemical symbol for Gold?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Ag", "Au", "Fe", "Pb"),
                        correctAnswers = listOf(1), // Au
                        explanation = "Derived from the Latin word 'Aurum'.",
                        hint = "Located in Group 11 of the periodic table.",
                        categories = listOf("Samples"),
                        additionalInfo = "Located in Group 11 of the periodic table.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "9bb082be-386e-43c7-8a74-56032e0f3c72",
                        quizId = englishQuizId,
                        text = "In what year did the Berlin Wall fall?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("1985", "1989", "1991", "1995"),
                        correctAnswers = listOf(1), // 1989
                        explanation = "This event marked the beginning of the end of the Cold War.",
                        hint = "Late eighties.",
                        categories = listOf("Samples"),
                        additionalInfo = "Late eighties.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "ad38d7bf-2579-43d7-86b0-a9a87216a0ea",
                        quizId = englishQuizId,
                        text = "The scale used to measure the magnitude of earthquakes is called:",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Fujita Scale", "Richter Scale", "Kelvin Scale", "Beaufort Scale"),
                        correctAnswers = listOf(1), // Richter Scale
                        explanation = "It is a logarithmic scale.",
                        hint = "Named after Charles F.",
                        categories = listOf("Samples"),
                        additionalInfo = "Named after Charles F.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "c73b95c4-e6e3-48b7-9696-332732ab7526",
                        quizId = englishQuizId,
                        text = "What was the first decentralized cryptocurrency introduced?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Ethereum", "Ripple", "Bitcoin", "Litecoin"),
                        correctAnswers = listOf(2), // Bitcoin
                        explanation = "Created by an unknown entity named Satoshi.",
                        hint = "Launched in the year 2009.",
                        categories = listOf("Samples"),
                        additionalInfo = "Launched in the year 2009.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "d31afbc9-6cdc-4e1f-a855-502b8f506f78",
                        quizId = englishQuizId,
                        text = "Who was the legendary king of Uruk that searched for immortality?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Zeus", "Odin", "Gilgamesh", "Enkidu"),
                        correctAnswers = listOf(2), // Gilgamesh
                        explanation = "Central figure of the world's oldest epic.",
                        hint = "His friend was named Enkidu.",
                        categories = listOf("Samples"),
                        additionalInfo = "His friend was named Enkidu.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "365abd50-636c-42e8-9ef7-6ba3d7ae225d",
                        quizId = englishQuizId,
                        text = "Which gas is most responsible for the greenhouse effect?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Oxygen", "Nitrogen", "Carbon Dioxide", "Helium"),
                        correctAnswers = listOf(2), // Carbon Dioxide
                        explanation = "Emitted primarily from burning fossil fuels.",
                        hint = "The gas we exhale.",
                        categories = listOf("Samples"),
                        additionalInfo = "The gas we exhale.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "b530119b-8ef9-4197-824b-0e2c7a48055e",
                        quizId = englishQuizId,
                        text = "Which country has won the most FIFA Men's World Cup titles?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Germany", "Italy", "Brazil", "Argentina"),
                        correctAnswers = listOf(2), // Brazil
                        explanation = "Crowned champions 5 times.",
                        hint = "The Samba team from South America.",
                        imagePath = "https://images.unsplash.com/photo-1489944440615-453fc2b6a9a9?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "The Samba team from South America.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "214f1f35-98e6-445c-9759-63200122c1ae",
                        quizId = englishQuizId,
                        text = "Which famous director helmed the sci-fi movie 'Inception'?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Steven Spielberg", "Quentin Tarantino", "Christopher Nolan", "James Cameron"),
                        correctAnswers = listOf(2), // Christopher Nolan
                        explanation = "A British director known for complex plots.",
                        hint = "Also directed Interstellar.",
                        categories = listOf("Samples"),
                        additionalInfo = "Also directed Interstellar.",
                        weight = 1,
                    ),
                ),
            )

            // --- Arabic Quiz ---
            val arabicQuizId =
                quizRepository.insertQuiz(
                    QuizEntity(
                        externalId = "16a396a8-d068-4305-9d4a-97f2577079d9",
                        bookId = bookId,
                        title = "نموذج اختبار",
                        description = "٢٥ سؤال في المعلومات العامة",
                        iconName = "🇮🇶",
                        category = "General",
                    ),
                )

            quizRepository.insertQuestions(
                listOf(
                    QuestionEntity(
                        externalId = "2737dea7-193f-485e-b657-e172adb5b083",
                        quizId = arabicQuizId,
                        text = "أي كوكب يُعرف بالكوكب الأحمر؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("المشتري", "المريخ", "الزهرة", "زحل"),
                        correctAnswers = listOf(1), // المريخ
                        explanation = "سطحه مغطى بأكسيد الحديد.",
                        hint = "هو الكوكب الرابع من الشمس.",
                        categories = listOf("Samples"),
                        additionalInfo = "هو الكوكب الرابع من الشمس.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "f802b2d4-4d28-48b0-99cc-b29f35d374ea",
                        quizId = arabicQuizId,
                        text = "ماذا يسمى المكون الأساسي لنظام تشغيل لينكس؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("الواجهة الرسومية", "النواة (Kernel)", "الصدفة (Shell)", "المحطة (Terminal)"),
                        correctAnswers = listOf(1), // النواة (Kernel)
                        explanation = "هو الجزء الذي يدير موارد النظام بالكامل.",
                        hint = "يبدأ بحرف K بالإنجليزية.",
                        categories = listOf("Samples"),
                        additionalInfo = "يبدأ بحرف K بالإنجليزية.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "48ae7af0-335f-4066-bdff-dc138d607b1a",
                        quizId = arabicQuizId,
                        text = "هذه البوابة الأثرية ذات الطوب الأزرق المزجج لأي مدينة كانت؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("سومر", "بابل", "أور", "نينوى"),
                        correctAnswers = listOf(1), // بابل
                        explanation = "بوابة عشتار في العراق القديم.",
                        hint = "بالقرب من مدينة الحلة.",
                        imagePath = "https://images.unsplash.com/photo-1599395232742-1e944b207238?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "بالقرب من مدينة الحلة.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "470ce5b1-1b47-4694-abd8-633d655d7bd3",
                        quizId = arabicQuizId,
                        text = "ما هما مصدرا الطاقة في السيارات الهجينة (HEV)؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("محرك احتراق ومحرك كهربائي", "طاقة شمسية وكهرباء", "هيدروجين وديزل", "كهرباء وطاقة رياح"),
                        correctAnswers = listOf(0), // محرك احتراق ومحرك كهربائي
                        explanation = "تستخدم الوقود والبطارية معاً لتوفير الطاقة.",
                        hint = "هي مزيج من نظامين.",
                        categories = listOf("Samples"),
                        additionalInfo = "هي مزيج من نظامين.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "7f1a77cc-d925-4b24-9ad4-4bd94d2a9991",
                        quizId = arabicQuizId,
                        text = "من هو مؤلف رواية '1984'؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("إرنست همينغوي", "جورج أورويل", "مارك توين", "تشارلز ديكنز"),
                        correctAnswers = listOf(1), // جورج أورويل
                        explanation = "رواية ديستوبيا شهيرة تناقش الشمولية.",
                        hint = "'الأخ الأكبر يراقبك'.",
                        categories = listOf("Samples"),
                        additionalInfo = "'الأخ الأكبر يراقبك'.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "88614d81-6e9c-40a9-8092-69eb71f9c494",
                        quizId = arabicQuizId,
                        text = "ما نوع تقنية الشاشات الشائعة في الهواتف القابلة للطي؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("LCD", "CRT", "OLED", "Plasma"),
                        correctAnswers = listOf(2), // OLED
                        explanation = "تعتمد على طبقات عضوية مرنة قابلة للانحناء.",
                        hint = "تبعث ضوءها الخاص بدون إضاءة خلفية.",
                        imagePath = "https://images.unsplash.com/photo-1585338107529-13afc5f02586?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "تبعث ضوءها الخاص بدون إضاءة خلفية.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "27b2a711-00ac-4c89-a676-89573757b86d",
                        quizId = arabicQuizId,
                        text = "كم عدد الحجرات في قلب الإنسان الطبيعي؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("2", "3", "4", "5"),
                        correctAnswers = listOf(2), // 4
                        explanation = "يتكون من أذينين وبطينين.",
                        hint = "اثنان في كل جانب.",
                        imagePath = "https://images.unsplash.com/photo-1530026405186-ed1f139313f8?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "اثنان في كل جانب.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "e7c82e2a-987e-4717-85e3-0f6a14e69adc",
                        quizId = arabicQuizId,
                        text = "أي مدينة مما يلي هي عاصمة اليابان؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("سيول", "بكين", "طوكيو", "بانكوك"),
                        correctAnswers = listOf(2), // طوكيو
                        explanation = "تعد أكبر منطقة حضرية في العالم من حيث عدد السكان.",
                        hint = "استضافت أولمبياد 2020.",
                        categories = listOf("Samples"),
                        additionalInfo = "استضافت أولمبياد 2020.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "2b04394d-f678-441f-9333-df1ad25fb10c",
                        quizId = arabicQuizId,
                        text = "طبق 'المسكوف' يشتهر به أي بلد؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("مصر", "العراق", "لبنان", "المغرب"),
                        correctAnswers = listOf(1), // العراق
                        explanation = "عبارة عن سمك شبوط مشوي على الحطب.",
                        hint = "مشهور في شارع أبو نواس.",
                        imagePath = "https://images.unsplash.com/photo-1628174542718-f2b74052345d?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "مشهور في شارع أبو نواس.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "4726b55f-bfd2-40bb-8001-8a79f7e4b382",
                        quizId = arabicQuizId,
                        text = "ما هو مصدر الطاقة الذي يولد الكهرباء باستخدام الخلايا الكهروضوئية؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("طاقة الرياح", "الطاقة الحرارية الأرضية", "الطاقة الشمسية", "الطاقة المائية"),
                        correctAnswers = listOf(2), // الطاقة الشمسية
                        explanation = "تستخرج الطاقة من ضوء الشمس المباشر.",
                        hint = "تأتي من السماء نهاراً.",
                        categories = listOf("Samples"),
                        additionalInfo = "تأتي من السماء نهاراً.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "d5869586-70b3-45f8-a0c1-145499c6d9b7",
                        quizId = arabicQuizId,
                        text = "إلى ماذا يرمز اختصار VPN؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("شبكة بصرية خاصة", "شبكة عامة افتراضية", "شبكة خاصة افتراضية", "شبكة بروتوكول متغير"),
                        correctAnswers = listOf(2), // شبكة خاصة افتراضية
                        explanation = "تقوم بتشفير حركة الإنترنت لحماية البيانات.",
                        hint = "تبدأ بكلمة 'افتراضية'.",
                        categories = listOf("Samples"),
                        additionalInfo = "تبدأ بكلمة 'افتراضية'.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "cb0c9b04-9508-49f9-89c0-4b7ced06cc88",
                        quizId = arabicQuizId,
                        text = "ما هي نسبة ضغطات الصدر إلى الأنفاس أثناء الإنعاش (CPR) للبالغين؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("15:2", "30:2", "10:1", "50:5"),
                        correctAnswers = listOf(1), // 30:2
                        explanation = "النسبة الموصى بها من جمعية القلب الأمريكية.",
                        hint = "30 ضغطة.",
                        categories = listOf("Samples"),
                        additionalInfo = "30 ضغطة.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "3858d7c5-6adc-4b1b-a493-b2b42d4cb75c",
                        quizId = arabicQuizId,
                        text = "من هو العالم الذي صاغ قوانين الحركة والجاذبية؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("ألبرت أينشتاين", "إسحاق نيوتن", "نيكولا تسلا", "غاليليو غاليلي"),
                        correctAnswers = listOf(1), // إسحاق نيوتن
                        explanation = "صاحب قصة التفاحة الشهيرة.",
                        hint = "له 3 قوانين أساسية للحركة.",
                        categories = listOf("Samples"),
                        additionalInfo = "له 3 قوانين أساسية للحركة.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "a73adf7d-92d2-4b85-883d-468001a34b5f",
                        quizId = arabicQuizId,
                        text = "ما هي لغة البرمجة المفضلة لدى جوجل لتطوير تطبيقات أندرويد؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("جافا", "سويفت", "كوتلن", "سي بلس بلس"),
                        correctAnswers = listOf(2), // كوتلن
                        explanation = "لغة حديثة وتعبيرية وتختصر الأكواد.",
                        hint = "أنشأتها شركة JetBrains.",
                        imagePath = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "أنشأتها شركة JetBrains.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "c4afa4b8-79ee-46a7-86db-8a12e32cd2aa",
                        quizId = arabicQuizId,
                        text = "لمن يُنسب فضل اختراع الهاتف؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("توماس إديسون", "ألكسندر جراهام بيل", "غولييلمو ماركوني", "جيمس واط"),
                        correctAnswers = listOf(1), // ألكسندر جراهام بيل
                        explanation = "سجل براءة اختراع أول هاتف عملي.",
                        hint = "ألكسندر جراهام...",
                        categories = listOf("Samples"),
                        additionalInfo = "ألكسندر جراهام...",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "23aeb9f1-dca8-48b4-bcfb-f28f624ad561",
                        quizId = arabicQuizId,
                        text = "أي مما يلي يُستخدم كمخدر وريدي سريع المفعول للحث على التخدير العام؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("براسيتامول", "بروبوفول", "أسبرين", "آيبوبروفين"),
                        correctAnswers = listOf(1), // بروبوفول
                        explanation = "يُستخدم بشكل واسع لبدء التخدير.",
                        hint = "يُلقب بـ 'حليب النسيان'.",
                        categories = listOf("Samples"),
                        additionalInfo = "يُلقب بـ 'حليب النسيان'.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "9f928f6a-9375-4a1f-9c4d-c7fbfb3490cc",
                        quizId = arabicQuizId,
                        text = "من رسم لوحة الموناليزا الشهيرة؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("فينسنت فان جوخ", "بابلو بيكاسو", "ليوناردو دا فينشي", "كلود مونيه"),
                        correctAnswers = listOf(2), // ليوناردو دا فينشي
                        explanation = "عالم وفنان إيطالي من عصر النهضة.",
                        hint = "ليوناردو...",
                        imagePath = "https://images.unsplash.com/photo-1577083165350-16c97a8b694f?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "ليوناردو...",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "f00b6c5d-b6b4-406f-8812-3cd014b40524",
                        quizId = arabicQuizId,
                        text = "ما هو الرمز الكيميائي للذهب؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Ag", "Au", "Fe", "Pb"),
                        correctAnswers = listOf(1), // Au
                        explanation = "مشتق من الكلمة اللاتينية Aurum.",
                        hint = "يقع في المجموعة 11 من الجدول الدوري.",
                        categories = listOf("Samples"),
                        additionalInfo = "يقع في المجموعة 11 من الجدول الدوري.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "21c91db8-8a8e-4b43-a648-4daa31663320",
                        quizId = arabicQuizId,
                        text = "في أي عام سقط جدار برلين؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("1985", "1989", "1991", "1995"),
                        correctAnswers = listOf(1), // 1989
                        explanation = "مثل هذا الحدث بداية نهاية الحرب الباردة.",
                        hint = "في أواخر الثمانينيات.",
                        categories = listOf("Samples"),
                        additionalInfo = "في أواخر الثمانينيات.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "9f99ce34-85c9-40dd-8de5-e82500472356",
                        quizId = arabicQuizId,
                        text = "ماذا يسمى المقياس المستخدم لقياس قوة الزلازل؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("مقياس فوجيتا", "مقياس ريختر", "مقياس كلفن", "مقياس بوفورت"),
                        correctAnswers = listOf(1), // مقياس ريختر
                        explanation = "هو مقياس لوغاريتمي يسجل قوة الهزات.",
                        hint = "سُمي على اسم تشارلز إف...",
                        categories = listOf("Samples"),
                        additionalInfo = "سُمي على اسم تشارلز إف...",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "35a59768-7702-4f51-adb0-ace138178257",
                        quizId = arabicQuizId,
                        text = "ما هي أول عملة مشفرة لامركزية تم إطلاقها؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("إيثيريوم", "ريبل", "بيتكوين", "لايتكوين"),
                        correctAnswers = listOf(2), // بيتكوين
                        explanation = "أنشأها شخص (أو مجموعة) مجهول باسم ساتوشي.",
                        hint = "انطلقت في عام 2009.",
                        categories = listOf("Samples"),
                        additionalInfo = "انطلقت في عام 2009.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "f11aea64-b13b-4b85-afc5-7032a0668bda",
                        quizId = arabicQuizId,
                        text = "من هو الملك الأسطوري لأوروك الذي بحث عن الخلود؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("زيوس", "أودين", "جلجامش", "إنكيدو"),
                        correctAnswers = listOf(2), // جلجامش
                        explanation = "تدور حوله أقدم ملحمة أدبية في العالم.",
                        hint = "صديقه اسمه إنكيدو.",
                        categories = listOf("Samples"),
                        additionalInfo = "صديقه اسمه إنكيدو.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "f853a4fd-268f-4c2c-a91a-0d4345e8eaf7",
                        quizId = arabicQuizId,
                        text = "ما هو الغاز الأكثر مسؤولية عن ظاهرة الاحتباس الحراري؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("الأكسجين", "النيتروجين", "ثاني أكسيد الكربون", "الهيليوم"),
                        correctAnswers = listOf(2), // ثاني أكسيد الكربون
                        explanation = "ينبعث بشكل رئيسي من حرق الوقود الأحفوري.",
                        hint = "الغاز الذي نطلقه عند الزفير.",
                        categories = listOf("Samples"),
                        additionalInfo = "الغاز الذي نطلقه عند الزفير.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "4c775cae-691f-4d35-8f08-08294c796237",
                        quizId = arabicQuizId,
                        text = "ما هو البلد الذي فاز بأكبر عدد من ألقاب كأس العالم؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("ألمانيا", "إيطاليا", "البرازيل", "الأرجنتين"),
                        correctAnswers = listOf(2), // البرازيل
                        explanation = "توج باللقب 5 مرات.",
                        hint = "فريق السامبا من أمريكا الجنوبية.",
                        imagePath = "https://images.unsplash.com/photo-1489944440615-453fc2b6a9a9?w=800&q=80",
                        categories = listOf("Samples"),
                        additionalInfo = "فريق السامبا من أمريكا الجنوبية.",
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "c62bef3f-2c65-4e31-922f-ab24b78b6250",
                        quizId = arabicQuizId,
                        text = "من هو مخرج فيلم الخيال العلمي 'Inception'؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("ستيفن سبيلبرغ", "كوينتن تارانتينو", "كريستوفر نولان", "جيمس كاميرون"),
                        correctAnswers = listOf(2), // كريستوفر نولان
                        explanation = "مخرج بريطاني معروف بحبكاته الزمنية المعقدة.",
                        hint = "أخرج أيضاً فيلم Interstellar.",
                        categories = listOf("Samples"),
                        additionalInfo = "أخرج أيضاً فيلم Interstellar.",
                        weight = 1,
                    ),
                ),
            )

            // --- How to Start Quiz (English) ---
            val howToStartEnId =
                quizRepository.insertQuiz(
                    QuizEntity(
                        externalId = "how-to-start-en",
                        bookId = bookId,
                        title = "How to start",
                        description = "Learn how to use the MKS app",
                        iconName = "🚀",
                        category = "Tutorial",
                    ),
                )

            quizRepository.insertQuestions(
                listOf(
                    QuestionEntity(
                        externalId = "tut-en-1",
                        quizId = howToStartEnId,
                        text = "What is MKS?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "A mobile knowledge system app for creating, importing, and taking quizzes",
                                "A music streaming application",
                                "A mail management service",
                                "A mobile keyboard software",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "MKS stands for Mobile Knowledge System, a comprehensive app for managing quizzes and study materials.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-2",
                        quizId = howToStartEnId,
                        text = "What does 'Rapid Mode' do during a quiz?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "Automatically submits and advances to the next question",
                                "Makes the questions harder",
                                "Deletes the quiz after finishing",
                                "Speeds up the background music",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "Rapid mode streamlines the quiz experience by auto-submitting correct answers and moving forward.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-3",
                        quizId = howToStartEnId,
                        text = "What is the purpose of the 'Categories' panel in the quiz player?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "To filter or tag questions by topic",
                                "To change the app theme",
                                "To see the local weather",
                                "To play mini-games",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "Categories help you organize and focus on specific subjects within a quiz.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-4",
                        quizId = howToStartEnId,
                        text = "How do you enable 'Double Tap to Answer'?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "It is on by default, but can be toggled in Settings",
                                "You must buy a premium version",
                                "By tapping the screen 10 times quickly",
                                "It is not a feature of this app",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "Double tap to submit is a convenience feature enabled by default for faster interaction.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-5",
                        quizId = howToStartEnId,
                        text = "How can you navigate between questions in a quiz?",
                        type = QuestionType.MULTIPLE_CHOICE,
                        options =
                            listOf(
                                "Using the bottom navigation toggles (Next/Previous)",
                                "Sliding the question card back and forth",
                                "Using 'Next' or 'Skip' buttons in the player sheet",
                                "By shaking the device",
                            ),
                        correctAnswers = listOf(0, 1, 2),
                        explanation = "MKS offers multiple ways to navigate: button toggles, swiping gestures, and the bottom sheet controls.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-6",
                        quizId = howToStartEnId,
                        text = "Where can you change the app's visual theme, font size, or UI density?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "In the Settings menu",
                                "By long-pressing the home screen",
                                "Themes are assigned randomly and cannot be changed",
                                "Through the 'Contact Us' banner",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "The Settings screen allows full customization of themes, font scaling, and UI spacing.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-7",
                        quizId = howToStartEnId,
                        text = "What is 'One by One' mode?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "A mode that reveals options one at a time as you tap 'Next'",
                                "A mode where you can only answer one question per day",
                                "A multiplayer mode for competing with friends",
                                "A mode that deletes questions after you answer them",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "One-by-one mode helps you focus by hiding upcoming options until you're ready to see them.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-8",
                        quizId = howToStartEnId,
                        text = "Which of the following is true about MKS import and export functions?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "You can export quizzes as single books or as whole libraries from the settings screen and you can import books from the \"+\" menu",
                                "Quizzes can only be imported via physical QR codes",
                                "Exporting is only possible to printers",
                                "Importing requires a paid subscription",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "MKS supports versatile import/export, making it easy to share and back up your study materials.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-9",
                        quizId = howToStartEnId,
                        text = "Which fields can be edited when modifying a Quiz or Book card?",
                        type = QuestionType.MULTIPLE_CHOICE,
                        options =
                            listOf(
                                "Title (Name)",
                                "Description",
                                "Icon or Cover Image",
                                "The actual paper weight of the book",
                            ),
                        correctAnswers = listOf(0, 1, 2),
                        explanation = "You can customize the metadata and imagery for any item in your library via the edit dialogs.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-10",
                        quizId = howToStartEnId,
                        text = "How do 'Categories' help you study in MKS?",
                        type = QuestionType.MULTIPLE_CHOICE,
                        options =
                            listOf(
                                "They allow you to practice only questions tagged with specific topics",
                                "They help sort and organize questions within a book",
                                "They can be used to generate entirely new quizzes from selected tags",
                                "They automatically answer questions for you",
                            ),
                        correctAnswers = listOf(0, 1, 2),
                        explanation = "Categories are powerful tools for focused practice and organizing large question sets.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-11",
                        quizId = howToStartEnId,
                        text = "How can you create a new quiz from existing questions?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "Use the '+' menu to combine multiple questions from the same book",
                                "By taking a photo of a textbook",
                                "Quizzes must be manually typed one word at a time",
                                "It is not possible to create custom quizzes",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "The '+' action menu allows you to create custom quizzes by selecting specific questions from your library.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-en-12",
                        quizId = howToStartEnId,
                        text = "If you have questions or need support, how should you reach out?",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "Use the 'Contact Us' banner on the home screen",
                                "Wait for the developer to call you",
                                "Search for it on a random forum",
                                "There is no support available",
                            ),
                        correctAnswers = listOf(0),
                        imagePath = "contact_banner",
                        explanation = "The developer is accessible via the Contact Us card for any feedback or help.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                ),
            )

            // --- How to Start Quiz (Arabic) ---
            val howToStartArId =
                quizRepository.insertQuiz(
                    QuizEntity(
                        externalId = "how-to-start-ar",
                        bookId = bookId,
                        title = "كيف تبدأ",
                        description = "تعلم كيفية استخدام تطبيق MKS",
                        iconName = "💡",
                        category = "Tutorial",
                    ),
                )

            quizRepository.insertQuestions(
                listOf(
                    QuestionEntity(
                        externalId = "tut-ar-1",
                        quizId = howToStartArId,
                        text = "ما هو تطبيق MKS؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "تطبيق نظام معرفة الجوال لإنشاء واستيراد وإجراء الاختبارات",
                                "تطبيق لتشغيل الموسيقى على الإنترنت",
                                "خدمة لإدارة البريد الإلكتروني",
                                "برنامج لوحة مفاتيح الهاتف المحمول",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "MKS اختصار لنظام معرفة الجوال، وهو تطبيق شامل لإدارة الاختبارات ومواد الدراسة.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-2",
                        quizId = howToStartArId,
                        text = "ماذا تفعل ميزة 'الوضع السريع' (Rapid Mode) أثناء الاختبار؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "تقوم بتسليم الإجابة والانتقال للسؤال التالي تلقائياً",
                                "تجعل الأسئلة أكثر صعوبة",
                                "تحذف الاختبار بعد الانتهاء منه",
                                "تسرع الموسيقى الخلفية",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "الوضع السريع يسهل تجربة الاختبار عبر أتمتة عملية التسليم والانتقال.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-3",
                        quizId = howToStartArId,
                        text = "ما هو الغرض من لوحة 'التصنيفات' (Categories) في مشغل الاختبار؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "تصفية أو وسم الأسئلة حسب الموضوع",
                                "تغيير مظهر التطبيق",
                                "معرفة حالة الطقس",
                                "لعب ألعاب مصغرة",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "تساعدك التصنيفات على تنظيم والتركيز على مواضيع محددة داخل الاختبار.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-4",
                        quizId = howToStartArId,
                        text = "كيف يتم تفعيل ميزة 'النقر المزدوج للإجابة'؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "مفعلة تلقائياً عند التثبيت، ويمكن تغييرها من الإعدادات",
                                "يجب شراء نسخة مدفوعة",
                                "عبر النقر على الشاشة 10 مرات بسرعة",
                                "ليست ميزة موجودة في التطبيق",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "النقر المزدوج ميزة مريحة مفعلة افتراضياً لتفاعل أسرع مع الخيارات.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-5",
                        quizId = howToStartArId,
                        text = "كيف يمكنك التنقل بين الأسئلة أثناء الاختبار؟",
                        type = QuestionType.MULTIPLE_CHOICE,
                        options =
                            listOf(
                                "باستخدام أزرار التنقل السفلية (التالي/السابق)",
                                "عبر سحب بطاقة السؤال يميناً ويساراً",
                                "باستخدام أزرار 'التالي' أو 'تخطي' في لوحة التحكم",
                                "عن طريق هز الهاتف",
                            ),
                        correctAnswers = listOf(0, 1, 2),
                        explanation = "يوفر MKS طرقاً متعددة للتنقل: الأزرار، الإيماءات بالسحب، وأزرار لوحة التحكم السفلية.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-6",
                        quizId = howToStartArId,
                        text = "أين يمكنك تغيير مظهر التطبيق، حجم الخط، أو كثافة الواجهة؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "من قائمة الإعدادات (Settings)",
                                "عبر النقر المطول على الشاشة الرئيسية",
                                "المظاهر يتم اختيارها عشوائياً ولا يمكن تغييرها",
                                "من خلال شريط 'تواصل معنا' فقط",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "تسمح لك شاشة الإعدادات بتخصيص المظهر وحجم الخط والمسافات في الواجهة بشكل كامل.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-7",
                        quizId = howToStartArId,
                        text = "ما هو وضع 'واحد تلو الآخر' (One by One)؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "وضع يظهر الخيارات واحداً تلو الآخر عند النقر على 'كشف'",
                                "وضع يسمح لك بالإجابة على سؤال واحد فقط يومياً",
                                "وضع للعب المتعدد مع الأصدقاء",
                                "وضع يقوم بحذف الأسئلة بعد الإجابة عليها",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "يساعدك هذا الوضع على التركيز عبر إخفاء الخيارات القادمة حتى تصبح جاهزاً لرؤيتها.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-8",
                        quizId = howToStartArId,
                        text = "أي مما يلي صحيح حول وظائف الاستيراد والتصدير في MKS؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "يمكنك تصدير الاختبارات ككتب منفردة أو تصدير المكتبة بالكامل من الاعدادات كما يمكن استيراد الكتب من قائمة \"+\"",
                                "يمكن استيراد الاختبارات فقط عبر رموز QR ورقية",
                                "التصدير متاح فقط للطابعات الورقية",
                                "الاستيراد يتطلب اشتراكاً مدفوعاً",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "يدعم التطبيق تنسيقات متعددة للاستيراد والتصدير لتسهيل مشاركة وحفظ موادك الدراسية.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-9",
                        quizId = howToStartArId,
                        text = "ما هي الحقول التي يمكن تعديلها عند تغيير بيانات بطاقة الكتاب أو الاختبار؟",
                        type = QuestionType.MULTIPLE_CHOICE,
                        options =
                            listOf(
                                "العنوان (الاسم)",
                                "الوصف",
                                "أيقونة الاختبار أو صورة غلاف الكتاب",
                                "وزن الورق الفعلي للكتاب",
                            ),
                        correctAnswers = listOf(0, 1, 2),
                        explanation = "يمكنك تخصيص البيانات الوصفية والصور لأي عنصر في مكتبتك من خلال نوافذ التعديل.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-10",
                        quizId = howToStartArId,
                        text = "كيف تساعدك 'التصنيفات' (Categories) في الدراسة؟",
                        type = QuestionType.MULTIPLE_CHOICE,
                        options =
                            listOf(
                                "تسمح لك بالتدرب على الأسئلة المرتبطة بمواضيع محددة فقط",
                                "تساعد في فرز وتنظيم الأسئلة داخل الكتاب",
                                "يمكن استخدامها لإنشاء اختبارات جديدة تماماً من وسوم مختارة",
                                "تقوم بالإجابة على الأسئلة نيابة عنك",
                            ),
                        correctAnswers = listOf(0, 1, 2),
                        explanation = "التصنيفات أدوات قوية للمذاكرة المركزة وتنظيم مجموعات الأسئلة الكبيرة.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-11",
                        quizId = howToStartArId,
                        text = "كيف يمكنك إنشاء اختبار جديد من أسئلة موجودة بالفعل؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "استخدام قائمة '+' لدمج عدة أسئلة من نفس الكتاب في اختبار جديد",
                                "عبر التقاط صورة للكتاب المدرسي",
                                "يجب كتابة الاختبار يدوياً كلمة بكلمة",
                                "لا يمكن إنشاء اختبارات مخصصة",
                            ),
                        correctAnswers = listOf(0),
                        explanation = "تتيح لك قائمة '+' إنشاء اختبارات مخصصة عبر اختيار أسئلة معينة من مكتبتك.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                    QuestionEntity(
                        externalId = "tut-ar-12",
                        quizId = howToStartArId,
                        text = "إذا كان لديك أسئلة أو كنت بحاجة للدعم، كيف يمكنك التواصل؟",
                        type = QuestionType.SINGLE_CHOICE,
                        options =
                            listOf(
                                "استخدم شريط 'تواصل معنا' في الشاشة الرئيسية",
                                "انتظر المطور ليتصل بك هاتفياً",
                                "ابحث عن ذلك في منتدى عشوائي",
                                "لا يوجد دعم متاح",
                            ),
                        correctAnswers = listOf(0),
                        imagePath = "contact_banner",
                        explanation = "المطور متاح عبر بطاقة 'تواصل معنا' لأي ملاحظات أو مساعدة.",
                        categories = listOf("How to start"),
                        weight = 1,
                    ),
                ),
            )

            // --- Knowledge Bank Assets ---

            // Sample Flashcard Deck
            val deckId = knowledgeRepository.insertFlashcardDeck(
                FlashcardDeckEntity(
                    bookId = bookId,
                    externalId = "sample_flashcard_deck",
                    title = "Basic Concepts Flashcards",
                    description = "Key facts from the General Knowledge quiz.",
                )
            )

            knowledgeRepository.insertFlashcards(
                listOf(
                    FlashcardEntity(
                        deckId = deckId,
                        externalId = "card_mars",
                        frontText = "Which planet is known as the Red Planet?",
                        backText = "Mars",
                        hint = "4th planet from the sun.",
                        orderIndex = 0
                    ),
                    FlashcardEntity(
                        deckId = deckId,
                        externalId = "card_linux",
                        frontText = "Core component of a Linux OS?",
                        backText = "Kernel",
                        hint = "Starts with K.",
                        orderIndex = 1
                    )
                )
            )

            // Sample Slideshow Course
            val courseId = knowledgeRepository.insertSlideshowCourse(
                SlideshowCourseEntity(
                    bookId = bookId,
                    externalId = "sample_slideshow_course",
                    title = "Introduction to Science & History",
                    description = "A visual guide to the sample questions.",
                )
            )

            knowledgeRepository.addCourseSlidesFromQuestionsToCourse(
                courseId = courseId,
                questionIds = listOf() // We'll add them manually to control content
            )
            
            // Manual Slides for better control in seeder
            knowledgeRepository.insertCourseSlide(
                com.ahmedyejam.mks.data.local.entity.CourseSlideEntity(
                    courseId = courseId,
                    externalId = "slide_intro",
                    title = "Welcome to MKS",
                    body = "This slideshow demonstrates how you can learn visually.",
                    orderIndex = 0
                )
            )

            // Sample Note Blueprint
            knowledgeRepository.insertNoteBlueprint(
                NoteBlueprintEntity(
                    collectionId = knowledgeRepository.getOrCreateDefaultNoteCollection(bookId),
                    externalId = "sample_note_blueprint",
                    title = "Study Note: Solar System",
                    summary = "A brief overview of planetary facts.",
                    body = "The Solar System consists of the Sun and everything that orbits it, including eight planets. Mars is often called the Red Planet due to iron oxide on its surface.",
                    bulletPoints = listOf("Mars is the 4th planet", "Jupiter is the largest", "Venus is the hottest"),
                    tags = listOf("Science", "Space"),
                    blueprintMode = BlueprintMode.CONCEPT_TEMPLATE
                )
            )
        }
    }
