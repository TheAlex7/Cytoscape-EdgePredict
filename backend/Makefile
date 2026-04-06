CC = gcc
OBJ_DIR = native_src/mock/_objs
MOCK_SRC_DIR = native_src/mock

all: $(OBJ_DIR) $(OBJ_DIR)/mock_blant

$(OBJ_DIR):
	mkdir -p $(OBJ_DIR)

$(OBJ_DIR)/mock_streams: $(MOCK_SRC_DIR)/mock_streams.c
	$(CC) $< -o $@

$(OBJ_DIR)/mock_blant: $(MOCK_SRC_DIR)/mock_blant.c
	$(CC) $< -o $@

clean:
	rm -rf $(OBJ_DIR)

.PHONY: all clean