CookMate - Тесты

ФИО: Басов Иван Алексеевич  
Группа: Б9123-09.03.03пикд

Юнит-тесты:
1. CookMateViewModelTest.kt - 9 тестов
	- testInitialState() - проверка начального состояния
	- testSearchMeals_Success() - успешная загрузка рецептов
	- testSearchMeals_Error() - обработка ошибки при загрузке
	- testRetryAfterError() - повторная попытка после ошибки
	- testSearchMeals_EmptyResult() - обработка пустого результата
	- testAllMeals_NoDuplicates() - отсутствие дубликатов при добавлении
	- testRetryActuallyInitiatesNewRequest() - retry реально делает второй запрос
	- testCancelOldRequest() - старый медленный запрос не перезаписывает новый результат
	- testFavoritesEmissionSequence() - правильная последовательность событий
	
2. MealRepositoryTest.kt - 4 теста
	- testSearchMealsByName_SuccessfulTransformation() - преобразование модели
	- testSearchMealsByName_EmptyResult() - обработка пустого результата от API
	- testGetMealDetails_Success() - успешная загрузка деталей
	- testGetMealDetails_NotFound() - обработка ошибки 404

3. CookMateViewModelFlowTest.kt - 3 Flow-теста
	- testFavoritesFlowEmissions() - проверка полной последовательности эмиссий
	- testNoExtraEmissions() - отсутствие лишних эмиссий при одинаковом значении
	- testOldRequestDoesNotOverrideNewResult() - старый отмененный запрос не перезаписывает новый результат

4. ViewModelContractTest.kt - 3 contract-теста
	- testSearchThenNavigateToDetail() - поиск и загрузка деталей по выбранному id
	- testErrorThenRetrySuccess() - после ошибки retry инициирует новый запрос
	- testStateTransitionsCorrectly() - переходы состояний Empty -> Success без зависания в Loading

5. ExampleUnitTest.kt - 1 тест
	- addition_isCorrect() - базовый системный тест

Интеграционные тесты (Android):
1. RoomIntegrationTest.kt - 4 теста
	- testSaveAndRetrieveFavouriteMeal() - сохранение и чтение рецепта из БД
	- testNoDuplicatesInFavourites() - PRIMARY KEY предотвращает дубликаты
	- testRemoveFromFavourites() - удаление рецепта из БД
	- testFavouritesOrderByAddedTime() - правильный порядок сортировки по дате

2. DataLayerIntegrationTest.kt - 2 теста
	- testAddMealToFavouritesAndObserve() - добавление в Flow и получение события
	- testFlowUpdatesOnDeletion() - обновление Flow при удалении

3. RepositoryRoomIntegrationTest.kt - 1 тест
	- repositoryResult_canBePersistedAndReadFromRoom() - связка Repository + Room в одной цепочке data-layer

4. UiNavigationIntegrationTest.kt - 1 тест
	- listItemClick_opensDetailForCorrectId() - реальный поиск на SearchScreen, клик по элементу, переход в реальный DetailScreen

5. ExampleInstrumentedTest.kt - 1 тест
	- useAppContext() - базовый системный тест

Покрытие сценариев:
1. Поиск рецепта по названию - успешно и с ошибками
2. Добавление/удаление из избранного с сохранением в Room
3. Retry при сетевых ошибках
4. Правильное отображение состояний: Loading, Content, Empty, Error
5. Отсутствие дубликатов при добавлении рецептов
6. Фильтрация пустых ингредиентов
7. Flow события - полная последовательность, отсутствие дублирования, отмена старых запросов
8. Room интеграция - сохранение, чтение, удаление, сортировка по дате
9. UI интеграция - список, клик по элементу, переход в детали нужного id
10. Интеграция data-layer - связка Repository + Room в одном сценарии

Всего тестов: 29
